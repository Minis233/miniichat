package com.miniichat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniichat.api.ChatMessage
import com.miniichat.api.LlmClient
import com.miniichat.data.AppSettings
import com.miniichat.data.Conversation
import com.miniichat.data.ConversationStore
import com.miniichat.data.Message
import com.miniichat.data.ProviderConfig
import com.miniichat.data.ProviderStore
import com.miniichat.data.SettingsRepository
import com.miniichat.util.newId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ConversationStore(app)
    val settingsRepo = SettingsRepository(app)
    val providerStore = ProviderStore(app)
    private val client = LlmClient()

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val providers: StateFlow<List<ProviderConfig>> = providerStore.providersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val conversations: StateFlow<List<Conversation>> = store.conversationsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _fetchingModelsFor = MutableStateFlow<String?>(null)
    val fetchingModelsFor: StateFlow<String?> = _fetchingModelsFor.asStateFlow()

    private var streamingJob: Job? = null

    fun selectConversation(id: String?) { _activeId.value = id }

    fun newConversation(): String {
        val id = newId()
        _activeId.value = id
        return id
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            store.delete(id)
            if (_activeId.value == id) _activeId.value = null
        }
    }

    fun renameConversation(id: String, title: String) {
        viewModelScope.launch { store.rename(id, title) }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        _isStreaming.value = false
    }

    fun clearError() { _error.value = null }
    fun clearToast() { _toast.value = null }

    fun activeProvider(): ProviderConfig? =
        providers.value.firstOrNull { it.id == settings.value.activeProviderId }

    fun selectModel(providerId: String, model: String) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(activeProviderId = providerId, activeModel = model) }
        }
    }

    fun upsertProvider(p: ProviderConfig) {
        viewModelScope.launch {
            providerStore.upsert(p)
            // If this is the first provider, mark active
            val all = providerStore.snapshot()
            if (settings.value.activeProviderId.isBlank() && all.isNotEmpty()) {
                val target = all.firstOrNull { it.id == p.id } ?: all.first()
                val firstModel = target.models.firstOrNull() ?: ""
                settingsRepo.update {
                    it.copy(activeProviderId = target.id, activeModel = firstModel)
                }
            }
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch {
            providerStore.delete(id)
            if (settings.value.activeProviderId == id) {
                val remaining = providerStore.snapshot()
                val nextProvider = remaining.firstOrNull()
                settingsRepo.update {
                    it.copy(
                        activeProviderId = nextProvider?.id ?: "",
                        activeModel = nextProvider?.models?.firstOrNull() ?: ""
                    )
                }
            }
        }
    }

    fun fetchModels(providerId: String) {
        viewModelScope.launch {
            val provider = providerStore.snapshot().firstOrNull { it.id == providerId } ?: return@launch
            _fetchingModelsFor.value = providerId
            try {
                val models = client.listModels(provider)
                if (models.isEmpty()) {
                    _toast.value = "No models returned from /models"
                } else {
                    val updated = provider.copy(models = (provider.models + models).distinct().sorted())
                    providerStore.upsert(updated)
                    _toast.value = "Fetched ${models.size} models"
                }
            } catch (e: Exception) {
                _error.value = "Fetch models failed: ${e.message}"
            } finally {
                _fetchingModelsFor.value = null
            }
        }
    }

    fun addManualModel(providerId: String, model: String) {
        viewModelScope.launch {
            val trimmed = model.trim()
            if (trimmed.isEmpty()) return@launch
            val provider = providerStore.snapshot().firstOrNull { it.id == providerId } ?: return@launch
            val updated = provider.copy(models = (provider.models + trimmed).distinct().sorted())
            providerStore.upsert(updated)
        }
    }

    fun removeModel(providerId: String, model: String) {
        viewModelScope.launch {
            val provider = providerStore.snapshot().firstOrNull { it.id == providerId } ?: return@launch
            val updated = provider.copy(models = provider.models - model)
            providerStore.upsert(updated)
            if (settings.value.activeProviderId == providerId && settings.value.activeModel == model) {
                settingsRepo.update {
                    it.copy(activeModel = updated.models.firstOrNull() ?: "")
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val provider = activeProvider()
        val current = settings.value
        if (provider == null) {
            _error.value = "No provider configured. Add one in Settings → Providers."
            return
        }
        if (provider.apiKey.isBlank() && !provider.baseUrl.contains("localhost") && !provider.baseUrl.contains("10.0.2.2")) {
            _error.value = "API key is empty for ${provider.name}."
            return
        }
        if (current.activeModel.isBlank()) {
            _error.value = "No model selected. Tap the model name in the top bar."
            return
        }

        viewModelScope.launch {
            val activeId = _activeId.value ?: newId().also { _activeId.value = it }
            val existing = store.snapshot().firstOrNull { it.id == activeId }
            val baseTitle = trimmed.take(30).replace("\n", " ")

            val userMsg = Message(id = newId(), role = "user", content = trimmed)
            val assistantId = newId()
            val assistantPlaceholder = Message(id = assistantId, role = "assistant", content = "")

            val updated = (existing ?: Conversation(id = activeId, title = baseTitle, messages = emptyList()))
                .let { conv ->
                    conv.copy(
                        title = if (conv.messages.isEmpty()) baseTitle else conv.title,
                        messages = conv.messages + userMsg + assistantPlaceholder,
                        updatedAt = System.currentTimeMillis()
                    )
                }
            store.upsert(updated)

            val historyForApi = mutableListOf<ChatMessage>()
            if (current.systemPrompt.isNotBlank()) {
                historyForApi.add(ChatMessage("system", current.systemPrompt))
            }
            updated.messages
                .filter { !(it.role == "assistant" && it.content.isEmpty()) }
                .forEach { historyForApi.add(ChatMessage(it.role, it.content)) }

            _isStreaming.value = true
            val builder = StringBuilder()

            streamingJob = launch {
                client.chatStream(provider, current, current.activeModel, historyForApi)
                    .catch { e ->
                        _error.value = e.message ?: "Request failed"
                        val finalContent = if (builder.isEmpty()) "(error: ${e.message})" else builder.toString()
                        appendAssistant(activeId, assistantId, finalContent)
                    }
                    .onEach { delta ->
                        builder.append(delta)
                        appendAssistant(activeId, assistantId, builder.toString())
                    }
                    .collect {}
            }
            streamingJob?.invokeOnCompletion { _isStreaming.value = false }
        }
    }

    private suspend fun appendAssistant(convId: String, msgId: String, content: String) {
        val list = store.snapshot()
        val conv = list.firstOrNull { it.id == convId } ?: return
        val newMsgs = conv.messages.map { if (it.id == msgId) it.copy(content = content) else it }
        store.upsert(conv.copy(messages = newMsgs, updatedAt = System.currentTimeMillis()))
    }

    fun regenerate() {
        viewModelScope.launch {
            val convId = _activeId.value ?: return@launch
            val conv = store.snapshot().firstOrNull { it.id == convId } ?: return@launch
            val msgs = conv.messages
            val lastUserIdx = msgs.indexOfLast { it.role == "user" }
            if (lastUserIdx < 0) return@launch
            val trimmed = msgs.subList(0, lastUserIdx + 1)
            store.upsert(conv.copy(messages = trimmed, updatedAt = System.currentTimeMillis()))
            sendMessage(msgs[lastUserIdx].content)
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepo.update(transform) }
    }
}

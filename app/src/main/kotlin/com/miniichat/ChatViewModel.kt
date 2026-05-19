package com.miniichat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniichat.api.ChatMessage
import com.miniichat.api.LlmClient
import com.miniichat.data.AppSettings
import com.miniichat.data.Assistant
import com.miniichat.data.AssistantStore
import com.miniichat.data.Conversation
import com.miniichat.data.ConversationStore
import com.miniichat.data.Message
import com.miniichat.data.ProviderConfig
import com.miniichat.data.ProviderStore
import com.miniichat.data.SettingsRepository
import com.miniichat.util.PromptVars
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
    val assistantStore = AssistantStore(app)
    private val client = LlmClient()

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val providers: StateFlow<List<ProviderConfig>> = providerStore.providersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val assistants: StateFlow<List<Assistant>> = assistantStore.assistantsFlow
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

    fun activeAssistant(): Assistant? =
        assistants.value.firstOrNull { it.id == settings.value.activeAssistantId }

    fun selectModel(providerId: String, model: String) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(activeProviderId = providerId, activeModel = model) }
        }
    }

    fun selectAssistant(id: String) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(activeAssistantId = id) }
        }
    }

    fun upsertAssistant(a: Assistant) {
        viewModelScope.launch { assistantStore.upsert(a) }
    }

    fun deleteAssistant(id: String) {
        viewModelScope.launch {
            assistantStore.delete(id)
            if (settings.value.activeAssistantId == id) {
                val remaining = assistantStore.snapshot()
                settingsRepo.update {
                    it.copy(activeAssistantId = remaining.firstOrNull()?.id ?: "default")
                }
            }
        }
    }

    fun upsertProvider(p: ProviderConfig) {
        viewModelScope.launch {
            providerStore.upsert(p)
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

    fun sendMessage(text: String, attachments: List<com.miniichat.data.Attachment> = emptyList()) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() && attachments.isEmpty()) return

        val current = settings.value
        val assistant = activeAssistant()

        // Resolve effective provider and model: assistant override > settings active
        val provider = assistant?.preferredProviderId
            ?.let { id -> providers.value.firstOrNull { it.id == id } }
            ?: activeProvider()
        if (provider == null) {
            _error.value = "No provider configured."
            return
        }

        val model = assistant?.preferredModel?.takeIf { it.isNotBlank() }
            ?: current.activeModel
        if (model.isBlank()) {
            _error.value = "No model selected."
            return
        }

        if (provider.apiKey.isBlank()
            && !provider.baseUrl.contains("localhost")
            && !provider.baseUrl.contains("10.0.2.2")
        ) {
            _error.value = "API key is empty for ${provider.name}."
            return
        }

        val temperature = assistant?.temperature ?: current.temperature

        val systemPromptRaw = assistant?.systemPrompt?.takeIf { it.isNotBlank() }
            ?: current.systemPrompt
        val systemPrompt = PromptVars.render(
            template = systemPromptRaw,
            model = model,
            provider = provider.name,
            assistant = assistant?.name ?: ""
        )

        viewModelScope.launch {
            val activeId = _activeId.value ?: newId().also { _activeId.value = it }
            val existing = store.snapshot().firstOrNull { it.id == activeId }
            val baseTitle = trimmed.take(30).replace("\n", " ")

            val userMsg = Message(
                id = newId(),
                role = "user",
                content = trimmed,
                attachments = attachments
            )
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
            if (systemPrompt.isNotBlank()) {
                historyForApi.add(ChatMessage("system", systemPrompt))
            }
            updated.messages
                .filter { !(it.role == "assistant" && it.content.isEmpty()) }
                .forEach { msg ->
                    val imgs = msg.attachments.filter { it.type == "image" }
                    if (msg.role == "user" && imgs.isNotEmpty()) {
                        // Build multipart content: text + image_url parts.
                        val parts = mutableListOf<com.miniichat.api.ChatPart>()
                        if (msg.content.isNotBlank()) {
                            parts.add(com.miniichat.api.ChatPart(type = "text", text = msg.content))
                        }
                        for (att in imgs) {
                            // Try to load + base64-encode the image. Fall back silently if it fails.
                            val loaded = runCatching {
                                com.miniichat.util.AttachmentLoader.load(
                                    resolver = getApplication<android.app.Application>().contentResolver,
                                    uri = android.net.Uri.parse(att.uri),
                                    mimeFallback = att.mimeType.ifBlank { "image/jpeg" }
                                )
                            }.getOrNull() ?: continue
                            val dataUrl = "data:${loaded.mimeType};base64,${loaded.base64}"
                            parts.add(com.miniichat.api.ChatPart(
                                type = "image_url",
                                imageUrl = com.miniichat.api.ChatPart.ImageUrl(url = dataUrl)
                            ))
                        }
                        // Also describe non-image attachments as text references.
                        val others = msg.attachments.filter { it.type != "image" }
                        if (others.isNotEmpty()) {
                            val tail = others.joinToString("\n") { "[file: ${it.name} (${it.mimeType})]" }
                            val merged = if (parts.firstOrNull()?.type == "text") {
                                parts[0] = com.miniichat.api.ChatPart(
                                    type = "text",
                                    text = (parts[0].text ?: "") + "\n\n" + tail
                                )
                                parts
                            } else {
                                listOf(com.miniichat.api.ChatPart(type = "text", text = tail)) + parts
                            }
                            historyForApi.add(ChatMessage(msg.role, msg.content, merged))
                        } else {
                            historyForApi.add(ChatMessage(msg.role, msg.content, parts))
                        }
                    } else if (msg.role == "user" && msg.attachments.isNotEmpty()) {
                        // Files only — describe inline as text refs.
                        val refs = msg.attachments.joinToString("\n") {
                            "[file: ${it.name} (${it.mimeType})]"
                        }
                        val combined = if (msg.content.isBlank()) refs else "${msg.content}\n\n$refs"
                        historyForApi.add(ChatMessage(msg.role, combined))
                    } else {
                        historyForApi.add(ChatMessage(msg.role, msg.content))
                    }
                }

            _isStreaming.value = true
            val builder = StringBuilder()

            // Use settings.copy with assistant temperature override
            val effectiveSettings = current.copy(temperature = temperature)

            streamingJob = launch {
                try {
                    client.chatStream(provider, effectiveSettings, model, historyForApi)
                        .catch { e ->
                            _error.value = e.message ?: "Request failed"
                            val finalContent = if (builder.isEmpty()) "(error: ${e.message})" else builder.toString()
                            appendAssistant(activeId, assistantId, finalContent)
                        }
                        .collect { delta ->
                            builder.append(delta)
                            appendAssistant(activeId, assistantId, builder.toString())
                        }
                } finally {
                    _isStreaming.value = false
                    streamingJob = null
                }
            }
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

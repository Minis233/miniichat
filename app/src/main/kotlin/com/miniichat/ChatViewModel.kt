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
    private val client = LlmClient()

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val conversations: StateFlow<List<Conversation>> = store.conversationsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var streamingJob: Job? = null

    fun selectConversation(id: String?) {
        _activeId.value = id
    }

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

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val current = settings.value
        if (current.apiKey.isBlank()) {
            _error.value = "No API key configured."
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

            // Build payload (system + history without empty assistant placeholder)
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
                client.chatStream(current, historyForApi)
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
            streamingJob?.invokeOnCompletion {
                _isStreaming.value = false
            }
        }
    }

    private suspend fun appendAssistant(convId: String, msgId: String, content: String) {
        val list = store.snapshot()
        val conv = list.firstOrNull { it.id == convId } ?: return
        val newMsgs = conv.messages.map {
            if (it.id == msgId) it.copy(content = content) else it
        }
        store.upsert(conv.copy(messages = newMsgs, updatedAt = System.currentTimeMillis()))
    }

    fun regenerate() {
        viewModelScope.launch {
            val convId = _activeId.value ?: return@launch
            val conv = store.snapshot().firstOrNull { it.id == convId } ?: return@launch
            val msgs = conv.messages
            // find last user message
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

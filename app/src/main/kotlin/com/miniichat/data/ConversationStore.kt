package com.miniichat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.conversationsDataStore: DataStore<Preferences> by preferencesDataStore(name = "conversations")

class ConversationStore(private val context: Context) {

    private val key = stringPreferencesKey("conversations_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val conversationsFlow: Flow<List<Conversation>> =
        context.conversationsDataStore.data.map { prefs ->
            val raw = prefs[key] ?: return@map emptyList()
            runCatching { json.decodeFromString<List<Conversation>>(raw) }.getOrDefault(emptyList())
        }

    suspend fun snapshot(): List<Conversation> = conversationsFlow.first()

    suspend fun save(list: List<Conversation>) {
        context.conversationsDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Conversation.serializer()), list)
        }
    }

    suspend fun upsert(conv: Conversation) {
        val current = snapshot().toMutableList()
        val idx = current.indexOfFirst { it.id == conv.id }
        if (idx >= 0) current[idx] = conv else current.add(0, conv)
        current.sortByDescending { it.updatedAt }
        save(current)
    }

    suspend fun delete(id: String) {
        save(snapshot().filterNot { it.id == id })
    }

    suspend fun rename(id: String, newTitle: String) {
        val current = snapshot().toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx >= 0) {
            current[idx] = current[idx].copy(title = newTitle, updatedAt = System.currentTimeMillis())
            save(current)
        }
    }
}

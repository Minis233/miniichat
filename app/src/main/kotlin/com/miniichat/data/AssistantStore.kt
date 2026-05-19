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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.assistantsDataStore: DataStore<Preferences> by preferencesDataStore(name = "assistants")

class AssistantStore(private val context: Context) {
    private val key = stringPreferencesKey("assistants_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val assistantsFlow: Flow<List<Assistant>> =
        context.assistantsDataStore.data.map { prefs ->
            val raw = prefs[key]
            if (raw.isNullOrBlank()) AssistantPresets.defaults()
            else runCatching {
                json.decodeFromString(ListSerializer(Assistant.serializer()), raw)
            }.getOrDefault(AssistantPresets.defaults())
        }

    suspend fun snapshot(): List<Assistant> = assistantsFlow.first()

    suspend fun save(list: List<Assistant>) {
        context.assistantsDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(ListSerializer(Assistant.serializer()), list)
        }
    }

    suspend fun upsert(a: Assistant) {
        val cur = snapshot().toMutableList()
        val idx = cur.indexOfFirst { it.id == a.id }
        if (idx >= 0) cur[idx] = a else cur.add(a)
        save(cur)
    }

    suspend fun delete(id: String) {
        save(snapshot().filterNot { it.id == id })
    }
}

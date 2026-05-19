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

private val Context.providersDataStore: DataStore<Preferences> by preferencesDataStore(name = "providers")

class ProviderStore(private val context: Context) {
    private val key = stringPreferencesKey("providers_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val providersFlow: Flow<List<ProviderConfig>> =
        context.providersDataStore.data.map { prefs ->
            val raw = prefs[key] ?: return@map emptyList()
            runCatching { json.decodeFromString(ListSerializer(ProviderConfig.serializer()), raw) }
                .getOrDefault(emptyList())
        }

    suspend fun snapshot(): List<ProviderConfig> = providersFlow.first()

    suspend fun save(list: List<ProviderConfig>) {
        context.providersDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(ListSerializer(ProviderConfig.serializer()), list)
        }
    }

    suspend fun upsert(p: ProviderConfig) {
        val list = snapshot().toMutableList()
        val idx = list.indexOfFirst { it.id == p.id }
        if (idx >= 0) list[idx] = p else list.add(p)
        save(list)
    }

    suspend fun delete(id: String) {
        save(snapshot().filterNot { it.id == id })
    }
}

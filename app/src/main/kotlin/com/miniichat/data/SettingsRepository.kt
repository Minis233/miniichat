package com.miniichat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val activeProviderId: String = "",
    val activeModel: String = "",
    val systemPrompt: String = "You are a helpful assistant.",
    val temperature: Float = 0.7f,
    val stream: Boolean = true,
    // Visual prefs
    val glassIntensity: Float = 0.6f
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val PROVIDER = stringPreferencesKey("active_provider_id")
        val MODEL = stringPreferencesKey("active_model")
        val SYSTEM = stringPreferencesKey("system_prompt")
        val TEMP = floatPreferencesKey("temperature")
        val STREAM = booleanPreferencesKey("stream")
        val GLASS = floatPreferencesKey("glass_intensity")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            activeProviderId = p[Keys.PROVIDER] ?: "",
            activeModel = p[Keys.MODEL] ?: "",
            systemPrompt = p[Keys.SYSTEM] ?: "You are a helpful assistant.",
            temperature = p[Keys.TEMP] ?: 0.7f,
            stream = p[Keys.STREAM] ?: true,
            glassIntensity = p[Keys.GLASS] ?: 0.6f
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { p ->
            val cur = AppSettings(
                activeProviderId = p[Keys.PROVIDER] ?: "",
                activeModel = p[Keys.MODEL] ?: "",
                systemPrompt = p[Keys.SYSTEM] ?: "You are a helpful assistant.",
                temperature = p[Keys.TEMP] ?: 0.7f,
                stream = p[Keys.STREAM] ?: true,
                glassIntensity = p[Keys.GLASS] ?: 0.6f
            )
            val next = transform(cur)
            p[Keys.PROVIDER] = next.activeProviderId
            p[Keys.MODEL] = next.activeModel
            p[Keys.SYSTEM] = next.systemPrompt
            p[Keys.TEMP] = next.temperature
            p[Keys.STREAM] = next.stream
            p[Keys.GLASS] = next.glassIntensity
        }
    }
}

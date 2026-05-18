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
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val systemPrompt: String = "You are a helpful assistant.",
    val temperature: Float = 0.7f,
    val stream: Boolean = true
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL = stringPreferencesKey("model")
        val SYSTEM = stringPreferencesKey("system_prompt")
        val TEMP = floatPreferencesKey("temperature")
        val STREAM = booleanPreferencesKey("stream")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            baseUrl = p[Keys.BASE_URL] ?: "https://api.openai.com/v1",
            apiKey = p[Keys.API_KEY] ?: "",
            model = p[Keys.MODEL] ?: "gpt-4o-mini",
            systemPrompt = p[Keys.SYSTEM] ?: "You are a helpful assistant.",
            temperature = p[Keys.TEMP] ?: 0.7f,
            stream = p[Keys.STREAM] ?: true
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { p ->
            val current = AppSettings(
                baseUrl = p[Keys.BASE_URL] ?: "https://api.openai.com/v1",
                apiKey = p[Keys.API_KEY] ?: "",
                model = p[Keys.MODEL] ?: "gpt-4o-mini",
                systemPrompt = p[Keys.SYSTEM] ?: "You are a helpful assistant.",
                temperature = p[Keys.TEMP] ?: 0.7f,
                stream = p[Keys.STREAM] ?: true
            )
            val next = transform(current)
            p[Keys.BASE_URL] = next.baseUrl
            p[Keys.API_KEY] = next.apiKey
            p[Keys.MODEL] = next.model
            p[Keys.SYSTEM] = next.systemPrompt
            p[Keys.TEMP] = next.temperature
            p[Keys.STREAM] = next.stream
        }
    }
}

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
    val activeAssistantId: String = "default",
    val systemPrompt: String = "You are a helpful assistant.",
    val temperature: Float = 0.7f,
    val stream: Boolean = true,
    val language: String = "system",          // system | en | zh
    val dynamicColor: Boolean = true,
    val themeMode: String = "system"          // system | light | dark
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val PROVIDER = stringPreferencesKey("active_provider_id")
        val MODEL = stringPreferencesKey("active_model")
        val ASSISTANT = stringPreferencesKey("active_assistant_id")
        val SYSTEM = stringPreferencesKey("system_prompt")
        val TEMP = floatPreferencesKey("temperature")
        val STREAM = booleanPreferencesKey("stream")
        val LANG = stringPreferencesKey("language")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val THEME = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p -> read(p) }

    private fun read(p: Preferences) = AppSettings(
        activeProviderId = p[Keys.PROVIDER] ?: "",
        activeModel = p[Keys.MODEL] ?: "",
        activeAssistantId = p[Keys.ASSISTANT] ?: "default",
        systemPrompt = p[Keys.SYSTEM] ?: "You are a helpful assistant.",
        temperature = p[Keys.TEMP] ?: 0.7f,
        stream = p[Keys.STREAM] ?: true,
        language = p[Keys.LANG] ?: "system",
        dynamicColor = p[Keys.DYNAMIC] ?: true,
        themeMode = p[Keys.THEME] ?: "system"
    )

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { p ->
            val next = transform(read(p))
            p[Keys.PROVIDER] = next.activeProviderId
            p[Keys.MODEL] = next.activeModel
            p[Keys.ASSISTANT] = next.activeAssistantId
            p[Keys.SYSTEM] = next.systemPrompt
            p[Keys.TEMP] = next.temperature
            p[Keys.STREAM] = next.stream
            p[Keys.LANG] = next.language
            p[Keys.DYNAMIC] = next.dynamicColor
            p[Keys.THEME] = next.themeMode
        }
    }
}

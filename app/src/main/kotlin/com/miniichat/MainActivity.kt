package com.miniichat

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.miniichat.ui.AppRoot
import com.miniichat.ui.theme.MiniiChatTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val vm: ChatViewModel by viewModels()

    override fun attachBaseContext(newBase: Context?) {
        if (newBase == null) { super.attachBaseContext(null); return }
        // Read language synchronously from a tiny SharedPreferences mirror written by SettingsRepository.
        val lang = newBase.getSharedPreferences("locale_cache", MODE_PRIVATE)
            .getString("language", "system") ?: "system"
        val ctx = if (lang == "system") newBase else applyLocale(newBase, lang)
        super.attachBaseContext(ctx)
    }

    private fun applyLocale(base: Context, lang: String): Context {
        val locale = when (lang) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }
        Locale.setDefault(locale)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(locale)
        return base.createConfigurationContext(cfg)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val s by vm.settings.collectAsState()
            // Mirror language to SharedPreferences so attachBaseContext can pick it up next launch
            val prefs = getSharedPreferences("locale_cache", MODE_PRIVATE)
            if (prefs.getString("language", "system") != s.language) {
                prefs.edit().putString("language", s.language).apply()
            }
            MiniiChatTheme(themeMode = s.themeMode, dynamicColor = s.dynamicColor) {
                AppRoot(vm)
            }
        }
    }
}

package com.miniichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniichat.R
import com.miniichat.data.AppSettings
import com.miniichat.data.Assistant
import com.miniichat.data.ProviderConfig

@Composable
fun SettingsScreen(
    settings: AppSettings,
    providers: List<ProviderConfig>,
    assistants: List<Assistant>,
    onBack: () -> Unit,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onOpenProviders: () -> Unit,
    onOpenAssistants: () -> Unit
) {
    var system by rememberSaveable(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }
    var temperature by rememberSaveable(settings.temperature) { mutableStateOf(settings.temperature) }
    var stream by rememberSaveable(settings.stream) { mutableStateOf(settings.stream) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        SettingsTopBar(title = stringResource(R.string.settings), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Providers entry
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenProviders)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.providers),
                            style = MaterialTheme.typography.titleMedium)
                        Text("${providers.size} configured",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Assistants entry
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAssistants)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.assistants),
                            style = MaterialTheme.typography.titleMedium)
                        val active = assistants.firstOrNull { it.id == settings.activeAssistantId }
                        Text(
                            active?.let { "${it.avatar}  ${it.name}" }
                                ?: "${assistants.size} assistants",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Behavior
            SectionHeader(stringResource(R.string.setting_system).let { "Behavior" })
            SectionCard {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(stringResource(R.string.setting_system),
                        style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    OutlinedFieldBox {
                        BasicTextField(
                            value = system,
                            onValueChange = {
                                system = it
                                onChange { s -> s.copy(systemPrompt = it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(
                                color = LocalContentColor.current,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            minLines = 2,
                            maxLines = 5
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "${stringResource(R.string.setting_temperature)}: ${"%.2f".format(temperature)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = temperature,
                        onValueChange = {
                            temperature = it
                            onChange { s -> s.copy(temperature = it) }
                        },
                        valueRange = 0f..2f,
                        steps = 19
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.setting_stream),
                                style = MaterialTheme.typography.bodyLarge)
                            Text("Server-Sent Events",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = stream,
                            onCheckedChange = {
                                stream = it
                                onChange { s -> s.copy(stream = it) }
                            }
                        )
                    }
                }
            }

            // Appearance
            SectionHeader(stringResource(R.string.setting_appearance))
            SectionCard {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.setting_dynamic_color),
                                style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.setting_dynamic_color_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = { v ->
                                onChange { s -> s.copy(dynamicColor = v) }
                            }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.setting_theme_mode),
                        style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    SegmentedRow(
                        options = listOf(
                            "system" to stringResource(R.string.setting_theme_system),
                            "light" to stringResource(R.string.setting_theme_light),
                            "dark" to stringResource(R.string.setting_theme_dark)
                        ),
                        selectedKey = settings.themeMode,
                        onSelect = { k -> onChange { s -> s.copy(themeMode = k) } }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.setting_language),
                        style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    SegmentedRow(
                        options = listOf(
                            "system" to stringResource(R.string.setting_language_system),
                            "en" to stringResource(R.string.setting_language_en),
                            "zh" to stringResource(R.string.setting_language_zh)
                        ),
                        selectedKey = settings.language,
                        onSelect = { k -> onChange { s -> s.copy(language = k) } }
                    )
                }
            }

            SectionHeader("About")
            SectionCard {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(stringResource(R.string.about_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SegmentedRow(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
    ) {
        options.forEach { (key, label) ->
            val selected = key == selectedKey
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface
                        else androidx.compose.ui.graphics.Color.Transparent
                    )
                    .clickable { onSelect(key) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) { content() }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun OutlinedFieldBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) { content() }
}

@Composable
fun SettingsTopBar(title: String, onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
    }
}

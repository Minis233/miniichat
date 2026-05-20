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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniichat.R
import com.miniichat.data.ProviderConfig
import com.miniichat.data.ProviderPresets
import com.miniichat.util.BaseUrlNormalizer
import com.miniichat.util.newId

/**
 * Full-screen provider editor.
 * Supports basic fields + custom HTTP headers + extra body params (kelivo/rikkahub style).
 */
@Composable
fun ProviderEditorScreen(
    initial: ProviderConfig?,
    onCancel: () -> Unit,
    onSave: (ProviderConfig) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var baseUrl by remember(initial?.id) { mutableStateOf(initial?.baseUrl ?: "") }
    var apiKey by remember(initial?.id) { mutableStateOf(initial?.apiKey ?: "") }
    var presetBaseUrl by remember(initial?.id) { mutableStateOf("") }
    var headerRows by remember(initial?.id) {
        mutableStateOf<List<Pair<String, String>>>(
            initial?.customHeaders?.map { it.key to it.value } ?: emptyList()
        )
    }
    var bodyRows by remember(initial?.id) {
        mutableStateOf<List<Pair<String, String>>>(
            initial?.extraBody?.map { it.key to it.value } ?: emptyList()
        )
    }
    var advancedOpen by remember(initial?.id) {
        mutableStateOf((initial?.customHeaders?.isNotEmpty() == true)
            || (initial?.extraBody?.isNotEmpty() == true))
    }
    var presetSheetOpen by remember(initial?.id) { mutableStateOf(initial == null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "cancel")
            }
            Text(
                if (initial == null) stringResource(R.string.add_provider)
                else stringResource(R.string.edit_provider),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                enabled = name.isNotBlank()
                    && (baseUrl.isNotBlank() || presetBaseUrl.isNotBlank() || initial?.baseUrl?.isNotBlank() == true),
                onClick = {
                    val effective = baseUrl.trim().ifEmpty {
                        presetBaseUrl.ifEmpty { initial?.baseUrl ?: "" }
                    }
                    val normalizedUrl = BaseUrlNormalizer.normalize(effective)
                    val headers = headerRows.filter { it.first.isNotBlank() }
                        .associate { it.first.trim() to it.second.trim() }
                    val body = bodyRows.filter { it.first.isNotBlank() }
                        .associate { it.first.trim() to it.second.trim() }
                    val p = (initial ?: ProviderConfig(
                        id = newId(),
                        name = name.trim(),
                        baseUrl = normalizedUrl,
                        apiKey = apiKey.trim()
                    )).copy(
                        name = name.trim(),
                        baseUrl = normalizedUrl,
                        apiKey = apiKey.trim(),
                        customHeaders = headers,
                        extraBody = body
                    )
                    onSave(p)
                }
            ) { Text(stringResource(R.string.save)) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            if (initial == null) {
                TextButton(onClick = { presetSheetOpen = true }) {
                    Text(stringResource(R.string.choose_preset))
                }
            }

            FieldSection(stringResource(R.string.provider_name)) {
                EditableValue(name, "") { name = it }
            }
            FieldSection(stringResource(R.string.setting_base_url)) {
                EditableValue(
                    value = baseUrl,
                    placeholder = presetBaseUrl.ifEmpty { initial?.baseUrl ?: "https://…/v1" },
                    monospace = true
                ) { baseUrl = it }
            }
            FieldSection(stringResource(R.string.setting_api_key)) {
                EditableValue(apiKey, "", monospace = true) { apiKey = it }
            }

            // Advanced — custom headers + extra body
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { advancedOpen = !advancedOpen }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.advanced_options),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (advancedOpen) Icons.Default.ExpandLess
                    else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (advancedOpen) {
                KeyValueEditor(
                    title = stringResource(R.string.custom_headers),
                    hint = stringResource(R.string.custom_headers_hint),
                    rows = headerRows,
                    onChange = { headerRows = it.toMutableList() }
                )
                KeyValueEditor(
                    title = stringResource(R.string.extra_body),
                    hint = stringResource(R.string.extra_body_hint),
                    rows = bodyRows,
                    onChange = { bodyRows = it.toMutableList() }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (presetSheetOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { presetSheetOpen = false },
            confirmButton = {
                TextButton(onClick = { presetSheetOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.pick_preset_or_manual)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProviderPresets.all.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (name.isBlank()) name = preset.name
                                    presetBaseUrl = preset.baseUrl
                                    presetSheetOpen = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Column {
                                Text(preset.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    preset.hint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun FieldSection(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun EditableValue(
    value: String,
    placeholder: String,
    monospace: Boolean = false,
    onChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
                )
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(
                color = LocalContentColor.current,
                fontSize = 14.sp,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true
        )
    }
}

@Composable
private fun KeyValueEditor(
    title: String,
    hint: String,
    rows: List<Pair<String, String>>,
    onChange: (List<Pair<String, String>>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        rows.forEachIndexed { index, (k, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    if (k.isEmpty()) {
                        Text(stringResource(R.string.key),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp))
                    }
                    BasicTextField(
                        value = k,
                        onValueChange = { newK ->
                            val nextList = rows.toMutableList()
                            nextList[index] = newK to v
                            onChange(nextList)
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = LocalContentColor.current,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    if (v.isEmpty()) {
                        Text(stringResource(R.string.value),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp))
                    }
                    BasicTextField(
                        value = v,
                        onValueChange = { newV ->
                            val nextList = rows.toMutableList()
                            nextList[index] = k to newV
                            onChange(nextList)
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = LocalContentColor.current,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                IconButton(
                    onClick = {
                        val nextList = rows.toMutableList()
                        nextList.removeAt(index)
                        onChange(nextList)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close, contentDescription = "remove",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                .clickable { onChange(rows + ("" to "")) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.add_row),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

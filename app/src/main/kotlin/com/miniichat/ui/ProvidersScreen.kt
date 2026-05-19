package com.miniichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniichat.R
import com.miniichat.data.ProviderConfig
import com.miniichat.data.ProviderPresets
import com.miniichat.util.BaseUrlNormalizer
import com.miniichat.util.newId

private const val MODELS_COLLAPSED_LIMIT = 5

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProvidersScreen(
    providers: List<ProviderConfig>,
    fetchingId: String?,
    activeProviderId: String,
    activeModel: String,
    onBack: () -> Unit,
    onUpsert: (ProviderConfig) -> Unit,
    onDelete: (String) -> Unit,
    onFetchModels: (String) -> Unit,
    onAddManualModel: (String, String) -> Unit,
    onRemoveModel: (String, String) -> Unit,
    onSelectModel: (String, String) -> Unit
) {
    var editing by remember { mutableStateOf<ProviderConfig?>(null) }
    var creating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        SettingsTopBar(stringResource(R.string.providers), onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { creating = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.add_provider),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            if (providers.isEmpty()) {
                SectionCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Storage, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.no_providers),
                            style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.no_providers_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                providers.forEach { p ->
                    ProviderCard(
                        provider = p,
                        fetching = fetchingId == p.id,
                        activeModel = if (activeProviderId == p.id) activeModel else "",
                        onEdit = { editing = p },
                        onDelete = { onDelete(p.id) },
                        onFetch = { onFetchModels(p.id) },
                        onAddManual = { m -> onAddManualModel(p.id, m) },
                        onRemoveModel = { m -> onRemoveModel(p.id, m) },
                        onSelectModel = { m -> onSelectModel(p.id, m) }
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (creating) {
        ProviderEditor(
            initial = null,
            onCancel = { creating = false },
            onSave = { onUpsert(it); creating = false }
        )
    }
    editing?.let { target ->
        ProviderEditor(
            initial = target,
            onCancel = { editing = null },
            onSave = { onUpsert(it); editing = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderCard(
    provider: ProviderConfig,
    fetching: Boolean,
    activeModel: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFetch: () -> Unit,
    onAddManual: (String) -> Unit,
    onRemoveModel: (String) -> Unit,
    onSelectModel: (String) -> Unit
) {
    var manualInput by remember { mutableStateOf("") }
    var deleteOpen by remember { mutableStateOf(false) }
    var modelsExpanded by remember(provider.id) { mutableStateOf(false) }

    SectionCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
                    Text(provider.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(provider.baseUrl,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { deleteOpen = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !fetching, onClick = onFetch)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (fetching) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Download, contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (fetching) stringResource(R.string.fetching)
                            else stringResource(R.string.fetch_models),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${provider.models.size} models",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedFieldBox {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(
                            color = LocalContentColor.current,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (manualInput.isEmpty()) {
                                Text(
                                    stringResource(R.string.add_model_manually),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            inner()
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    val canAdd = manualInput.trim().isNotEmpty()
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (canAdd) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                            )
                            .clickable(enabled = canAdd) {
                                onAddManual(manualInput.trim())
                                manualInput = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add, contentDescription = "add",
                            modifier = Modifier.size(16.dp),
                            tint = if (canAdd) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (provider.models.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val total = provider.models.size
                val showAll = modelsExpanded || total <= MODELS_COLLAPSED_LIMIT
                val visible = if (showAll) provider.models else provider.models.take(MODELS_COLLAPSED_LIMIT)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    visible.forEach { m ->
                        ModelChip(
                            label = m,
                            selected = m == activeModel,
                            onSelect = { onSelectModel(m) },
                            onRemove = { onRemoveModel(m) }
                        )
                    }
                }
                if (total > MODELS_COLLAPSED_LIMIT) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { modelsExpanded = !modelsExpanded }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (modelsExpanded) Icons.Default.ExpandLess
                                else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (modelsExpanded)
                                    stringResource(R.string.collapse_models)
                                else
                                    stringResource(R.string.expand_models, total - MODELS_COLLAPSED_LIMIT),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.no_models),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (deleteOpen) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            confirmButton = {
                TextButton(onClick = { onDelete(); deleteOpen = false }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteOpen = false }) { Text(stringResource(R.string.cancel)) }
            },
            title = { Text("Delete provider?") },
            text = { Text("Remove ${provider.name}? Its model list will be lost.") }
        )
    }
}

@Composable
private fun ModelChip(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onSelect)
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 4.dp)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(22.dp)) {
            Icon(
                Icons.Default.Close, contentDescription = "remove",
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProviderEditor(
    initial: ProviderConfig?,
    onCancel: () -> Unit,
    onSave: (ProviderConfig) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    // baseUrl + apiKey here are *user input* — we never write the preset URL into them.
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(initial?.apiKey ?: "") }
    var presetMenuOpen by remember { mutableStateOf(initial == null) }
    // Selected preset only used as placeholder + fallback default.
    var presetBaseUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank()
                    && (baseUrl.isNotBlank() || presetBaseUrl.isNotBlank() || initial?.baseUrl?.isNotBlank() == true),
                onClick = {
                    val effective = baseUrl.trim().ifEmpty {
                        presetBaseUrl.ifEmpty { initial?.baseUrl ?: "" }
                    }
                    val normalizedUrl = BaseUrlNormalizer.normalize(effective)
                    val p = (initial ?: ProviderConfig(
                        id = newId(),
                        name = name.trim(),
                        baseUrl = normalizedUrl,
                        apiKey = apiKey.trim()
                    )).copy(
                        name = name.trim(),
                        baseUrl = normalizedUrl,
                        apiKey = apiKey.trim()
                    )
                    onSave(p)
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        },
        title = {
            Text(if (initial == null) stringResource(R.string.add_provider)
                else stringResource(R.string.edit_provider))
        },
        text = {
            Column {
                if (presetMenuOpen && initial == null) {
                    Text(
                        stringResource(R.string.pick_preset_or_manual),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProviderPresets.all.forEach { preset ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Just remember which preset was picked so we
                                        // can use its baseUrl as placeholder/default.
                                        // Don't overwrite the user's input fields.
                                        if (name.isBlank()) name = preset.name
                                        presetBaseUrl = preset.baseUrl
                                        presetMenuOpen = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
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
                } else {
                    DialogField(stringResource(R.string.provider_name), name, "") { name = it }
                    Spacer(Modifier.height(8.dp))
                    DialogField(
                        label = stringResource(R.string.setting_base_url),
                        value = baseUrl,
                        placeholder = presetBaseUrl.ifEmpty { initial?.baseUrl ?: "https://…/v1" }
                    ) { baseUrl = it }
                    Spacer(Modifier.height(8.dp))
                    DialogField(
                        stringResource(R.string.setting_api_key),
                        apiKey,
                        ""
                    ) { apiKey = it }
                    if (initial == null) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { presetMenuOpen = true }) {
                            Text(stringResource(R.string.choose_preset))
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun DialogField(
    label: String,
    value: String,
    placeholder: String = "",
    onChange: (String) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedFieldBox {
            Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp, fontFamily = FontFamily.Monospace
                        )
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        color = LocalContentColor.current,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true
                )
            }
        }
    }
}

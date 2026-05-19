package com.miniichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.graphics.Color
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
import com.miniichat.util.newId

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

            // Add provider button
            GlassSurface(
                shape = RoundedCornerShape(20.dp),
                tintAlpha = 0.55f,
                borderAlpha = 0.40f,
                modifier = Modifier.fillMaxWidth().clickable { creating = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.add_provider),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (providers.isEmpty()) {
                GlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    tintAlpha = 0.35f,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
            onSave = {
                onUpsert(it)
                creating = false
            }
        )
    }
    editing?.let { target ->
        ProviderEditor(
            initial = target,
            onCancel = { editing = null },
            onSave = {
                onUpsert(it)
                editing = null
            }
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

    GlassSurface(
        shape = RoundedCornerShape(20.dp),
        tintAlpha = 0.50f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
                    Text(
                        provider.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        provider.baseUrl,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { deleteOpen = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Fetch + manual add row
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassSurface(
                    shape = RoundedCornerShape(12.dp),
                    tintAlpha = 0.55f,
                    modifier = Modifier
                        .clickable(enabled = !fetching, onClick = onFetch)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

            // Manual input row
            GlassFieldBox {
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    provider.models.forEach { m ->
                        ModelChip(
                            label = m,
                            selected = m == activeModel,
                            onSelect = { onSelectModel(m) },
                            onRemove = { onRemoveModel(m) }
                        )
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
                TextButton(onClick = { deleteOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
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
    GlassSurface(
        shape = RoundedCornerShape(50),
        tintAlpha = if (selected) 0.65f else 0.30f,
        borderAlpha = if (selected) 0.45f else 0.22f,
        modifier = Modifier.clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
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
}

@Composable
fun ProviderEditor(
    initial: ProviderConfig?,
    onCancel: () -> Unit,
    onSave: (ProviderConfig) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(initial?.apiKey ?: "") }
    var presetMenuOpen by remember { mutableStateOf(initial == null) }

    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && baseUrl.isNotBlank(),
                onClick = {
                    val p = (initial ?: ProviderConfig(
                        id = newId(),
                        name = name.trim(),
                        baseUrl = baseUrl.trim(),
                        apiKey = apiKey.trim()
                    )).copy(
                        name = name.trim(),
                        baseUrl = baseUrl.trim(),
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
                        "Pick a preset or fill in manually:",
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
                                        name = preset.name
                                        baseUrl = preset.baseUrl
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
                    DialogField(stringResource(R.string.provider_name), name) { name = it }
                    Spacer(Modifier.height(8.dp))
                    DialogField(stringResource(R.string.setting_base_url), baseUrl) { baseUrl = it }
                    Spacer(Modifier.height(8.dp))
                    DialogField(stringResource(R.string.setting_api_key), apiKey) { apiKey = it }
                    if (initial == null) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { presetMenuOpen = true }) {
                            Text("Choose preset…")
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun DialogField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        GlassFieldBox {
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

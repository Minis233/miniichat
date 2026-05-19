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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniichat.R
import com.miniichat.data.Assistant
import com.miniichat.util.newId

@Composable
fun AssistantsScreen(
    assistants: List<Assistant>,
    activeId: String,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onUpsert: (Assistant) -> Unit,
    onDelete: (String) -> Unit
) {
    var editing by remember { mutableStateOf<Assistant?>(null) }
    var creating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        SettingsTopBar(stringResource(R.string.assistants), onBack)
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
                    Text(
                        stringResource(R.string.add_assistant),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            assistants.forEach { a ->
                AssistantRow(
                    assistant = a,
                    selected = a.id == activeId,
                    canDelete = assistants.size > 1,
                    onSelect = { onSelect(a.id) },
                    onEdit = { editing = a },
                    onDelete = { onDelete(a.id) }
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (creating) {
        AssistantEditor(
            initial = null,
            onCancel = { creating = false },
            onSave = { onUpsert(it); creating = false }
        )
    }
    editing?.let { target ->
        AssistantEditor(
            initial = target,
            onCancel = { editing = null },
            onSave = { onUpsert(it); editing = null }
        )
    }
}

@Composable
private fun AssistantRow(
    assistant: Assistant,
    selected: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(assistant.avatar, fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    assistant.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (selected) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            stringResource(R.string.active),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Text(
                assistant.systemPrompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "edit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete, enabled = canDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "delete",
                tint = if (canDelete) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
fun AssistantEditor(
    initial: Assistant?,
    onCancel: () -> Unit,
    onSave: (Assistant) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var avatar by remember(initial?.id) { mutableStateOf(initial?.avatar ?: "🤖") }
    var systemPrompt by remember(initial?.id) { mutableStateOf(initial?.systemPrompt ?: "") }
    var hasTemp by remember(initial?.id) { mutableStateOf(initial?.temperature != null) }
    var temp by remember(initial?.id) { mutableStateOf(initial?.temperature ?: 0.7f) }

    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val a = (initial ?: Assistant(id = newId(), name = name.trim()))
                        .copy(
                            name = name.trim(),
                            avatar = avatar.trim().ifBlank { "🤖" },
                            systemPrompt = systemPrompt,
                            temperature = if (hasTemp) temp else null
                        )
                    onSave(a)
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        },
        title = {
            Text(if (initial == null) stringResource(R.string.add_assistant)
                else stringResource(R.string.edit_assistant))
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { Text(avatar, fontSize = 22.sp) }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.assistant_name),
                            style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(2.dp))
                        OutlinedFieldBox {
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it },
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
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.assistant_avatar),
                    style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(2.dp))
                OutlinedFieldBox {
                    BasicTextField(
                        value = avatar,
                        onValueChange = { avatar = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            color = LocalContentColor.current,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.assistant_system_prompt),
                    style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(2.dp))
                OutlinedFieldBox {
                    BasicTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            color = LocalContentColor.current,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        minLines = 3,
                        maxLines = 7
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.prompt_vars_hint),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Switch(
                        checked = hasTemp,
                        onCheckedChange = { hasTemp = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.override_temperature),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium
                    )
                    if (hasTemp) {
                        Text("%.2f".format(temp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (hasTemp) {
                    Slider(
                        value = temp,
                        onValueChange = { temp = it },
                        valueRange = 0f..2f,
                        steps = 19
                    )
                }
            }
        }
    )
}

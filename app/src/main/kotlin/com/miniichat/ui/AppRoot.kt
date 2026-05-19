package com.miniichat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniichat.ChatViewModel
import com.miniichat.R
import com.miniichat.data.AppSettings
import com.miniichat.data.Conversation
import com.miniichat.data.Message
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: ChatViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val conversations by vm.conversations.collectAsState()
    val activeId by vm.activeId.collectAsState()
    val settings by vm.settings.collectAsState()
    val isStreaming by vm.isStreaming.collectAsState()
    val error by vm.error.collectAsState()

    val activeConv = conversations.firstOrNull { it.id == activeId }

    if (showSettings) {
        SettingsScreen(
            settings = settings,
            onBack = { showSettings = false },
            onChange = { vm.updateSettings(it) }
        )
        return
    }

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearError(); showSettings = true
                }) { Text(stringResource(R.string.settings)) }
            },
            dismissButton = {
                TextButton(onClick = { vm.clearError() }) { Text(stringResource(R.string.ok)) }
            },
            title = { Text("Error") },
            text = { Text(msg) }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                conversations = conversations,
                activeId = activeId,
                onSelect = { id ->
                    vm.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onNew = {
                    vm.newConversation()
                    scope.launch { drawerState.close() }
                },
                onDelete = { vm.deleteConversation(it) },
                onRename = { id, t -> vm.renameConversation(id, t) },
                onOpenSettings = {
                    showSettings = true
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        ChatScreen(
            conversation = activeConv,
            settings = settings,
            isStreaming = isStreaming,
            onMenu = { scope.launch { drawerState.open() } },
            onSend = { vm.sendMessage(it) },
            onStop = { vm.stopStreaming() },
            onRegenerate = { vm.regenerate() },
            onNew = { vm.newConversation() },
            onOpenSettings = { showSettings = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerContent(
    conversations: List<Conversation>,
    activeId: String?,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var query by remember { mutableStateOf("") }

    val filtered = remember(conversations, query) {
        if (query.isBlank()) conversations
        else conversations.filter { it.title.contains(query, ignoreCase = true) }
    }
    val grouped = remember(filtered) { groupConversationsByDate(filtered) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxSize().width(312.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(WindowInsets.statusBars.asPaddingValues())
                .fillMaxSize()
        ) {
            // Header — brand + new chat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("M", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onNew) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.new_chat))
                }
            }

            // Search
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    "Search chats",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (query.isBlank()) "No chats yet" else "No matches",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    grouped.forEach { (label, items) ->
                        item(key = "h-$label") {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(items, key = { it.id }) { conv ->
                            ChatRow(
                                conv = conv,
                                selected = conv.id == activeId,
                                onClick = { onSelect(conv.id) },
                                onRename = { renameTarget = conv },
                                onDelete = { deleteTarget = conv }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.padding(WindowInsets.navigationBars.asPaddingValues()))
        }
    }

    renameTarget?.let { conv ->
        var newTitle by remember(conv.id) { mutableStateOf(conv.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    onRename(conv.id, newTitle.ifBlank { "Untitled" })
                    renameTarget = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
            title = { Text(stringResource(R.string.rename)) },
            text = {
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, singleLine = true)
            }
        )
    }
    deleteTarget?.let { conv ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            confirmButton = {
                TextButton(onClick = { onDelete(conv.id); deleteTarget = null }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text("Delete \"${conv.title}\"?") }
        )
    }
}

@Composable
private fun ChatRow(
    conv: Conversation,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                conv.title.ifBlank { "Untitled" },
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "more",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    conversation: Conversation?,
    settings: AppSettings,
    isStreaming: Boolean,
    onMenu: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onRegenerate: () -> Unit,
    onNew: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val messages = conversation?.messages ?: emptyList()

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = settings.model.ifBlank { stringResource(R.string.app_name) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (conversation != null && conversation.title.isNotBlank()) {
                            Text(
                                text = conversation.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNew) {
                        Icon(Icons.Default.Edit, contentDescription = "new chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            if (messages.isEmpty()) {
                EmptyState(
                    onPick = { onSend(it) },
                    onOpenSettings = onOpenSettings,
                    showSettingsHint = settings.apiKey.isBlank(),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageItem(
                            message = msg,
                            isLastAssistant = msg == messages.lastOrNull() && msg.role == "assistant",
                            isStreaming = isStreaming
                        )
                    }
                }
            }

            // Regenerate button when last message is a finished assistant reply
            AnimatedVisibility(
                visible = messages.isNotEmpty() &&
                    messages.last().role == "assistant" &&
                    !isStreaming &&
                    messages.last().content.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable(onClick = onRegenerate)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.regenerate),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            InputBar(
                value = input,
                onValueChange = { input = it },
                onSend = {
                    val text = input
                    input = ""
                    onSend(text)
                },
                onStop = onStop,
                isStreaming = isStreaming
            )
        }
    }
}

@Composable
private fun MessageItem(message: Message, isLastAssistant: Boolean, isStreaming: Boolean) {
    when (message.role) {
        "user" -> UserMessage(message)
        else -> AssistantMessage(message, isLastAssistant, isStreaming)
    }
}

@Composable
private fun UserMessage(message: Message) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 6.dp
            ),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            SelectionContainer {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun AssistantMessage(message: Message, isLastAssistant: Boolean, isStreaming: Boolean) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) { kotlinx.coroutines.delay(1200); copied = false }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Avatar
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "M",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (message.content.isEmpty() && isLastAssistant && isStreaming) {
                TypingIndicator()
            } else {
                SelectionContainer {
                    MarkdownText(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            // Per-message copy action (assistant only, after streaming finishes)
            if (message.content.isNotEmpty() && (!isLastAssistant || !isStreaming)) {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            clipboard.setText(AnnotatedString(message.content))
                            copied = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "copy",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (copied) "Copied" else stringResource(R.string.copy),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infinite = rememberInfiniteTransition(label = "typing")
    val alpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (i == 0) alpha else if (i == 1) (1f - alpha) else alpha * 0.7f + 0.3f
                        )
                    )
            )
        }
    }
}

@Composable
private fun EmptyState(
    onPick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    showSettingsHint: Boolean,
    modifier: Modifier = Modifier
) {
    val examples = listOf(
        stringResource(R.string.example_prompt_1),
        stringResource(R.string.example_prompt_2),
        stringResource(R.string.example_prompt_3),
        stringResource(R.string.example_prompt_4)
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text("M", color = MaterialTheme.colorScheme.onPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (showSettingsHint) {
            Spacer(Modifier.height(20.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.clickable(onClick = onOpenSettings)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.error_no_key),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            examples.forEach { example ->
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(example) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            example,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isStreaming: Boolean
) {
    val focus = LocalFocusManager.current
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues())
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(26.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Attach (placeholder — disabled hint, kept for visual parity)
                    IconButton(
                        onClick = { /* TODO: attachments not yet supported */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "attach",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp, max = 160.dp)
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                stringResource(R.string.hint_input),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(
                                color = LocalContentColor.current,
                                fontSize = 16.sp,
                                lineHeight = 22.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            maxLines = 6
                        )
                    }

                    val canSend = value.trim().isNotEmpty() && !isStreaming
                    val sendBg = when {
                        isStreaming -> MaterialTheme.colorScheme.onSurface
                        canSend -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                    val sendFg = when {
                        isStreaming -> MaterialTheme.colorScheme.surface
                        canSend -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(sendBg)
                            .clickable(enabled = isStreaming || canSend) {
                                if (isStreaming) onStop()
                                else if (canSend) {
                                    focus.clearFocus(); onSend()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isStreaming) Icons.Default.Stop else Icons.Default.ArrowUpward,
                            contentDescription = if (isStreaming) "stop" else "send",
                            tint = sendFg,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onChange: ((AppSettings) -> AppSettings) -> Unit
) {
    var baseUrl by rememberSaveable(settings.baseUrl) { mutableStateOf(settings.baseUrl) }
    var apiKey by rememberSaveable(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var model by rememberSaveable(settings.model) { mutableStateOf(settings.model) }
    var system by rememberSaveable(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }
    var temperature by rememberSaveable(settings.temperature) { mutableStateOf(settings.temperature) }
    var stream by rememberSaveable(settings.stream) { mutableStateOf(settings.stream) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = baseUrl, onValueChange = { baseUrl = it },
                label = { Text(stringResource(R.string.setting_base_url)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it },
                label = { Text(stringResource(R.string.setting_api_key)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = model, onValueChange = { model = it },
                label = { Text(stringResource(R.string.setting_model)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = system, onValueChange = { system = it },
                label = { Text(stringResource(R.string.setting_system)) },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4
            )
            Column {
                Text(
                    "${stringResource(R.string.setting_temperature)}: ${"%.2f".format(temperature)}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0f..2f,
                    steps = 19
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.setting_stream),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(checked = stream, onCheckedChange = { stream = it })
            }
            Button(
                onClick = {
                    onChange { it.copy(
                        baseUrl = baseUrl.trim(),
                        apiKey = apiKey.trim(),
                        model = model.trim(),
                        systemPrompt = system,
                        temperature = temperature,
                        stream = stream
                    ) }
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.ok)) }
            HorizontalDivider()
            Text(stringResource(R.string.setting_about), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.about_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---- helpers ----
private fun groupConversationsByDate(list: List<Conversation>): List<Pair<String, List<Conversation>>> {
    if (list.isEmpty()) return emptyList()
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()
    cal.timeInMillis = now
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis
    val yesterdayStart = todayStart - 86_400_000L
    val sevenDaysStart = todayStart - 7 * 86_400_000L
    val thirtyDaysStart = todayStart - 30 * 86_400_000L

    val today = mutableListOf<Conversation>()
    val yesterday = mutableListOf<Conversation>()
    val sevenDays = mutableListOf<Conversation>()
    val thirtyDays = mutableListOf<Conversation>()
    val older = mutableListOf<Conversation>()

    list.sortedByDescending { it.updatedAt }.forEach { c ->
        when {
            c.updatedAt >= todayStart -> today += c
            c.updatedAt >= yesterdayStart -> yesterday += c
            c.updatedAt >= sevenDaysStart -> sevenDays += c
            c.updatedAt >= thirtyDaysStart -> thirtyDays += c
            else -> older += c
        }
    }

    val out = mutableListOf<Pair<String, List<Conversation>>>()
    if (today.isNotEmpty()) out += "Today" to today
    if (yesterday.isNotEmpty()) out += "Yesterday" to yesterday
    if (sevenDays.isNotEmpty()) out += "Previous 7 days" to sevenDays
    if (thirtyDays.isNotEmpty()) out += "Previous 30 days" to thirtyDays
    if (older.isNotEmpty()) out += "Older" to older
    return out
}

// end of file

package com.miniichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.miniichat.ChatViewModel
import com.miniichat.R
import kotlinx.coroutines.launch

private enum class Screen { Chat, Settings, Providers, Assistants }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: ChatViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var screen by rememberSaveable { mutableStateOf(Screen.Chat) }
    var showModelPicker by rememberSaveable { mutableStateOf(false) }

    // System back: drawer closes drawer; sub-screens go back to parent;
    // model picker dismisses; otherwise default (exit app).
    androidx.activity.compose.BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    androidx.activity.compose.BackHandler(enabled = showModelPicker) {
        showModelPicker = false
    }
    androidx.activity.compose.BackHandler(enabled = screen == Screen.Settings) {
        screen = Screen.Chat
    }
    androidx.activity.compose.BackHandler(enabled = screen == Screen.Providers) {
        screen = Screen.Settings
    }
    androidx.activity.compose.BackHandler(enabled = screen == Screen.Assistants) {
        screen = Screen.Settings
    }

    val conversations by vm.conversations.collectAsState()
    val activeId by vm.activeId.collectAsState()
    val settings by vm.settings.collectAsState()
    val providers by vm.providers.collectAsState()
    val isStreaming by vm.isStreaming.collectAsState()
    val error by vm.error.collectAsState()
    val toast by vm.toast.collectAsState()
    val fetchingId by vm.fetchingModelsFor.collectAsState()

    val activeConv = conversations.firstOrNull { it.id == activeId }
    val activeProvider = providers.firstOrNull { it.id == settings.activeProviderId }
    val assistants by vm.assistants.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(toast) {
        toast?.let {
            snackbar.showSnackbar(it, duration = SnackbarDuration.Short)
            vm.clearToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (screen) {
            Screen.Chat -> {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        GlassDrawer(
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
                                screen = Screen.Settings
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                    ChatScreen(
                        conversation = activeConv,
                        settings = settings,
                        activeProvider = activeProvider,
                        isStreaming = isStreaming,
                        onMenu = { scope.launch { drawerState.open() } },
                        onSend = { text, atts -> vm.sendMessage(text, atts) },
                        onStop = { vm.stopStreaming() },
                        onRegenerate = { vm.regenerate() },
                        onNew = { vm.newConversation() },
                        onOpenSettings = { screen = Screen.Settings },
                        onPickModel = {
                            if (providers.isEmpty()) screen = Screen.Providers
                            else showModelPicker = true
                        }
                    )
                }
            }
            Screen.Settings -> {
                SettingsScreen(
                    settings = settings,
                    providers = providers,
                    assistants = assistants,
                    onBack = { screen = Screen.Chat },
                    onChange = { vm.updateSettings(it) },
                    onOpenProviders = { screen = Screen.Providers },
                    onOpenAssistants = { screen = Screen.Assistants }
                )
            }
            Screen.Assistants -> {
                AssistantsScreen(
                    assistants = assistants,
                    activeId = settings.activeAssistantId,
                    onBack = { screen = Screen.Settings },
                    onSelect = { vm.selectAssistant(it) },
                    onUpsert = { vm.upsertAssistant(it) },
                    onDelete = { vm.deleteAssistant(it) }
                )
            }
            Screen.Providers -> {
                ProvidersScreen(
                    providers = providers,
                    fetchingId = fetchingId,
                    activeProviderId = settings.activeProviderId,
                    activeModel = settings.activeModel,
                    onBack = { screen = Screen.Settings },
                    onUpsert = { vm.upsertProvider(it) },
                    onDelete = { vm.deleteProvider(it) },
                    onFetchModels = { vm.fetchModels(it) },
                    onAddManualModel = { id, m -> vm.addManualModel(id, m) },
                    onRemoveModel = { id, m -> vm.removeModel(id, m) },
                    onSelectModel = { id, m -> vm.selectModel(id, m) }
                )
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.fillMaxSize())
    }

    if (showModelPicker) {
        ModelPickerSheet(
            providers = providers,
            activeProviderId = settings.activeProviderId,
            activeModel = settings.activeModel,
            onPick = { pid, m -> vm.selectModel(pid, m) },
            onDismiss = { showModelPicker = false }
        )
    }

    error?.let { msg ->
        // Only offer "Open Providers" when the error is about provider/model config.
        val needsProviderFix = msg.contains("provider", ignoreCase = true)
            || msg.contains("api key", ignoreCase = true)
            || msg.contains("model", ignoreCase = true)
            || msg.contains("401")
            || msg.contains("403")
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            confirmButton = {
                if (needsProviderFix) {
                    TextButton(onClick = {
                        vm.clearError()
                        screen = Screen.Providers
                    }) { Text(stringResource(R.string.error_open_providers)) }
                } else {
                    TextButton(onClick = { vm.clearError() }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            },
            dismissButton = if (needsProviderFix) {
                {
                    TextButton(onClick = { vm.clearError() }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            } else null,
            title = { Text(stringResource(R.string.error_dialog_title)) },
            text = { Text(msg) }
        )
    }
}

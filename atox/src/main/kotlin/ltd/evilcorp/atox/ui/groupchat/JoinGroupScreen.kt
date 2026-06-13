// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.groupchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.AtoxLoadingButton
import ltd.evilcorp.atox.ui.common.AtoxPasswordField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

private const val FOCUS_REQUEST_DELAY_MS = 100L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(
    onBack: () -> Unit,
    isJoiningState: StateFlow<Boolean>,
    onValidateChatId: (String) -> String?,
    onJoinGroup: suspend (chatIdHex: String, password: String?) -> Boolean,
) {
    var chatIdHex by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isJoining by isJoiningState.collectAsState()

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(FOCUS_REQUEST_DELAY_MS)
        focusRequester.requestFocus()
    }

    val isScrolled by remember { derivedStateOf { scrollState.value > 0 } }
    val transitionAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isScrolled) 0.85f else 0.0f,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
        label = "topBarAlpha"
    )
    val transitionElevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isScrolled) 4.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
        label = "topBarElevation"
    )

    val performHaptic = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Surface(
                tonalElevation = transitionElevation,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = transitionAlpha),
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.join_group),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, enabled = !isJoining) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.join_group),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = stringResource(R.string.join_group_instructions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = chatIdHex,
                            onValueChange = { input ->
                                // Auto-strip clipboard inputs of whitespaces and newline characters during paste operations!
                                val cleanInput = input.replace("\\s".toRegex(), "")
                                chatIdHex = cleanInput.filter { c -> c in "0123456789abcdefABCDEF" }
                                errorMessage = null
                            },
                            label = { Text(stringResource(R.string.group_chat_id)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            enabled = !isJoining,
                            shape = MaterialTheme.shapes.medium,
                            isError = errorMessage != null,
                            supportingText = {
                                if (errorMessage != null) {
                                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        AtoxPasswordField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = stringResource(R.string.group_password),
                            placeholder = stringResource(R.string.group_password_placeholder),
                            enabled = !isJoining,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        AtoxLoadingButton(
                            onClick = {
                                val validationError = onValidateChatId(chatIdHex)
                                if (validationError != null) {
                                    errorMessage = validationError
                                    return@AtoxLoadingButton
                                }
                                performHaptic()
                                scope.launch {
                                    onJoinGroup(chatIdHex, password.takeIf { it.isNotBlank() })
                                }
                            },
                            text = stringResource(R.string.join_group),
                            isLoading = isJoining,
                            enabled = chatIdHex.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }
        }
    }
}

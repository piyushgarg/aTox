// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.groupchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.AtoxLoadingButton
import ltd.evilcorp.atox.ui.common.AtoxPasswordField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

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
        focusRequester.requestFocus()
    }

    val performHaptic = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
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
                modifier = Modifier.fillMaxWidth(),
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

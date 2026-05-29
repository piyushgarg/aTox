// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.groupchat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import ltd.evilcorp.atox.R
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import ltd.evilcorp.atox.ui.common.AtoxLoadingButton
import ltd.evilcorp.atox.ui.common.AtoxPasswordField
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState

private const val FOCUS_REQUEST_DELAY_MS = 100L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    isCreatingState: StateFlow<Boolean>,
    onCreateGroup: suspend (String, GroupPrivacyState, String?) -> Boolean,
) {
    var groupName by remember { mutableStateOf("") }
    var privacyState by remember { mutableStateOf(GroupPrivacyState.Public) }
    var password by remember { mutableStateOf("") }

    val isCreating by isCreatingState.collectAsState()

    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
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
                            text = stringResource(R.string.create_group),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, enabled = !isCreating) {
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
                        modifier = Modifier
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.create_group),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text(stringResource(R.string.group_name)) },
                            placeholder = { Text(stringResource(R.string.group_name_placeholder)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            enabled = !isCreating,
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.group_privacy),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Elegant M3 Segmented Control inside the Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val publicSelected = privacyState == GroupPrivacyState.Public
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            if (publicSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .clickable(enabled = !isCreating) {
                                            performHaptic()
                                            privacyState = GroupPrivacyState.Public
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.group_public),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (publicSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            if (!publicSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .clickable(enabled = !isCreating) {
                                            performHaptic()
                                            privacyState = GroupPrivacyState.Private
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.group_private),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!publicSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Premium drop-down transition visibility animation for private group password entry field!
                        AnimatedVisibility(
                            visible = privacyState == GroupPrivacyState.Private,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                        ) {
                            AtoxPasswordField(
                                value = password,
                                onValueChange = { password = it },
                                label = stringResource(R.string.group_password),
                                placeholder = stringResource(R.string.group_password_placeholder),
                                enabled = !isCreating,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        AtoxLoadingButton(
                            onClick = {
                                if (groupName.isNotBlank() && !isCreating) {
                                    performHaptic()
                                    scope.launch {
                                        onCreateGroup(groupName.trim(), privacyState, password.takeIf { it.isNotBlank() })
                                    }
                                }
                            },
                            text = stringResource(R.string.create),
                            isLoading = isCreating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = groupName.isNotBlank()
                        )
                    }
                }
            }
        }
    }
}

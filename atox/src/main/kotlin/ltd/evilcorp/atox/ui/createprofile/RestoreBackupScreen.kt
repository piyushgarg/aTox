package ltd.evilcorp.atox.ui.createprofile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.AtoxPasswordField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreBackupScreen(
    viewModel: CreateProfileViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var errorText by remember { mutableStateOf("") }
    var backupPassword by remember { mutableStateOf("") }
    var showGoogleDriveDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val googleBackups by viewModel.googleBackups.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        when (state) {
            is CreateProfileUiState.Success -> {
                keyboardController?.hide()
                onSuccess()
            }
            is CreateProfileUiState.Error -> {
                keyboardController?.hide()
                val errorResId = (state as CreateProfileUiState.Error).errorResId
                errorText = context.getString(errorResId)
            }
            else -> {}
        }
    }

    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && state !is CreateProfileUiState.Loading) {
            viewModel.restoreBackup(uri.toString(), backupPassword.takeIf { it.isNotBlank() })
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            try {
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null) {
                    viewModel.listGoogleDriveBackups()
                    showGoogleDriveDialog = true
                    return@rememberLauncherForActivityResult
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                android.util.Log.e("RestoreBackup", "Google Sign-In API Exception: ${e.statusCode}", e)
                val msg = when (e.statusCode) {
                    com.google.android.gms.common.api.CommonStatusCodes.DEVELOPER_ERROR ->
                        context.getString(R.string.google_sign_in_developer_error)
                    else ->
                        context.getString(R.string.google_sign_in_error_code, e.statusCode)
                }
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
        }

        if (result.resultCode != android.app.Activity.RESULT_OK) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.google_sign_in_cancelled),
                android.widget.Toast.LENGTH_LONG
            ).show()
        } else {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.google_sign_in_failed),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    if (showGoogleDriveDialog) {
        GoogleDriveRestoreDialog(
            backups = googleBackups,
            onDismiss = { showGoogleDriveDialog = false },
            onBackupSelected = { fileId, password ->
                showGoogleDriveDialog = false
                viewModel.restoreGoogleDriveBackup(fileId, password)
            },
            isLoading = state is CreateProfileUiState.Loading
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_restore_from_file)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.navigation_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.backup_restore_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = stringResource(R.string.backup_restore_confirm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (errorText.isNotEmpty()) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AtoxPasswordField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = stringResource(R.string.backup_password_optional),
                        enabled = state !is CreateProfileUiState.Loading,
                        shape = MaterialTheme.shapes.medium,
                        imeAction = ImeAction.Done,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            backupPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                        },
                        enabled = state !is CreateProfileUiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.backup_restore_from_file_row))
                    }

                    OutlinedButton(
                        onClick = {
                            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                            if (account != null) {
                                viewModel.listGoogleDriveBackups()
                                showGoogleDriveDialog = true
                            } else {
                                val googleSignInOptions = ltd.evilcorp.atox.infrastructure.backup.google.GoogleDriveBackupHelper.getSignInOptions()
                                val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, googleSignInOptions)
                                googleSignInLauncher.launch(client.signInIntent)
                            }
                        },
                        enabled = state !is CreateProfileUiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.backup_restore_from_google_drive))
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleDriveRestoreDialog(
    backups: List<ltd.evilcorp.domain.features.backup.model.CloudBackupInfo>,
    onDismiss: () -> Unit,
    onBackupSelected: (String, String?) -> Unit,
    isLoading: Boolean,
) {
    var selectedBackupId by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore_from_google_drive)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (backups.isEmpty()) {
                    Text(stringResource(R.string.backup_none_found))
                } else {
                    backups.forEach { backup ->
                        OutlinedButton(
                            onClick = { selectedBackupId = backup.id },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(backup.name)
                                Text(
                                    stringResource(R.string.backup_size_format, backup.sizeKb / 1024, backup.sizeKb),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    if (selectedBackupId != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AtoxPasswordField(
                            value = password,
                            onValueChange = { password = it },
                            label = stringResource(R.string.backup_password_optional),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedBackupId != null) {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onBackupSelected(selectedBackupId!!, password.takeIf { it.isNotBlank() })
                    },
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.backup_restore_from_file))
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.avatar_editor_cancel))
            }
        }
    )
}

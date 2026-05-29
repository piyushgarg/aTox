package ltd.evilcorp.atox.ui.createprofile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.atox.ui.common.AtoxPasswordField
import ltd.evilcorp.atox.ui.common.AtoxLoadingButton
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CreateProfileScreen(
    viewModel: CreateProfileViewModel,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var errorText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

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

    CreateProfileContent(
        isLoading = state is CreateProfileUiState.Loading,
        errorText = errorText,
        onErrorChanged = { errorText = it },
        onCreateProfile = { name ->
            keyboardController?.hide()
            viewModel.createProfile(name)
        },
        onRestoreBackup = { uriString, password ->
            keyboardController?.hide()
            viewModel.restoreBackup(uriString, password)
        }
    )
}

@Composable
fun CreateProfileContent(
    isLoading: Boolean,
    errorText: String,
    onErrorChanged: (String) -> Unit = {},
    onCreateProfile: (String) -> Unit = {},
    onRestoreBackup: (String, String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf("") }
    var backupPassword by remember { mutableStateOf("") }

    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !isLoading) {
            onRestoreBackup(uri.toString(), backupPassword.takeIf { it.isNotBlank() })
        }
    }

    val submitProfile = {
        if (nameInput.trim().isEmpty()) {
            onErrorChanged(context.getString(R.string.create_profile_error_empty))
        } else {
            onCreateProfile(nameInput.trim())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.ime)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.create_profile_welcome),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.create_profile_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        if (it.isNotEmpty()) onErrorChanged("")
                    },
                    label = { Text(stringResource(R.string.create_profile_username_label)) },
                    placeholder = { Text(stringResource(R.string.create_profile_username_placeholder)) },
                    isError = errorText.isNotEmpty(),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitProfile() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                AtoxLoadingButton(
                    onClick = { submitProfile() },
                    text = stringResource(R.string.create_profile_btn),
                    isLoading = isLoading,
                    enabled = nameInput.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                AtoxPasswordField(
                    value = backupPassword,
                    onValueChange = { backupPassword = it },
                    label = stringResource(R.string.backup_password_optional),
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                AtoxLoadingButton(
                    onClick = { backupPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                    text = stringResource(R.string.backup_restore_from_file),
                    isLoading = isLoading,
                    isOutlined = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateProfilePreview() {
    MaterialTheme {
        CreateProfileContent(
            isLoading = false,
            errorText = ""
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateProfileLoadingPreview() {
    MaterialTheme {
        CreateProfileContent(
            isLoading = true,
            errorText = ""
        )
    }
}

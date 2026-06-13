package ltd.evilcorp.atox.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.atox.ui.navigation.components.UnlockScreenContent

@Composable
fun LaunchScreen(
    viewModel: AuthViewModel,
    onLaunchResolved: (ToxSaveStatus) -> Unit,
) {
    val state by viewModel.launchState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadToxAsync(null)
    }

    LaunchedEffect(state) {
        if (state is LaunchUiState.Success) {
            onLaunchResolved((state as LaunchUiState.Success).status)
        }
    }

    when (state) {
        is LaunchUiState.Loading -> {
            LaunchScreenContent()
        }
        is LaunchUiState.Timeout -> {
            LaunchTimeoutScreen(onRetry = {
                scope.launch {
                    viewModel.loadToxAsync(null)
                }
            })
        }
        is LaunchUiState.Success -> {
            LaunchScreenContent()
        }
    }
}

@Composable
fun LaunchTimeoutScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "aTox",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.launch_timeout_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.launch_timeout_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
fun LaunchScreenContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "aTox",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.secure_connection_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Context.findActivity(): androidx.fragment.app.FragmentActivity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is androidx.fragment.app.FragmentActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

@Composable
fun UnlockScreen(
    viewModel: AuthViewModel,
    onUnlockSuccess: () -> Unit,
    onQuit: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isBiometricEnabled = remember(context) { ltd.evilcorp.atox.infrastructure.security.BiometricStorage.isBiometricEnabled(context) }
    
    val unlockState by viewModel.unlockState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(unlockState) {
        if (unlockState is UnlockUiState.Success) {
            onUnlockSuccess()
        }
    }

    val showBiometricPrompt = {
        if (activity != null) {
            val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
            val biometricPrompt = androidx.biometric.BiometricPrompt(
                activity,
                executor,
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                    }

                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        val cipher = result.cryptoObject?.cipher
                        if (cipher != null) {
                            val password = viewModel.decryptPassword(context, cipher)
                            if (password != null) {
                                scope.launch {
                                    val success = viewModel.unlockProfileAsync(password)
                                    if (success) {
                                        viewModel.enableBiometric(context, password)
                                    }
                                }
                            }
                        }
                    }
                }
            )

            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.unlock_profile_title))
                .setSubtitle(context.getString(R.string.unlock_profile_desc))
                .setNegativeButtonText(context.getString(android.R.string.cancel))
                .build()

            try {
                val iv = ltd.evilcorp.atox.infrastructure.security.BiometricStorage.getIv(context)
                if (iv != null) {
                    val cipher = ltd.evilcorp.atox.infrastructure.security.BiometricCipherHelper.getInitializedCipherForDecryption(iv)
                    biometricPrompt.authenticate(promptInfo, androidx.biometric.BiometricPrompt.CryptoObject(cipher))
                }
            } catch (e: Exception) {
                android.util.Log.e("UnlockScreen", "Failed to start biometric auth: $e")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isBiometricEnabled) {
            showBiometricPrompt()
        }
    }

    UnlockScreenContent(
        isError = unlockState is UnlockUiState.Error,
        isLoading = unlockState is UnlockUiState.Loading,
        isBiometricEnabled = isBiometricEnabled,
        onBiometricClick = { showBiometricPrompt() },
        onSubmitUnlock = { password ->
            scope.launch {
                val success = viewModel.unlockProfileAsync(password)
                if (success) {
                    viewModel.enableBiometric(context, password)
                }
            }
        },
        onQuit = onQuit,
        onClearError = { viewModel.clearUnlockError() }
    )
}

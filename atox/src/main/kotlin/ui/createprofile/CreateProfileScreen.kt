package ltd.evilcorp.atox.ui.createprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.core.tox.save.ToxSaveStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(
    onCreateProfile: (String) -> ToxSaveStatus
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
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
                        if (it.isNotEmpty()) errorText = ""
                    },
                    label = { Text(stringResource(R.string.create_profile_username_label)) },
                    placeholder = { Text(stringResource(R.string.create_profile_username_placeholder)) },
                    isError = errorText.isNotEmpty(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
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

                Button(
                    onClick = {
                        if (nameInput.trim().isEmpty()) {
                            errorText = context.getString(R.string.create_profile_error_empty)
                        } else {
                            errorText = when (onCreateProfile(nameInput.trim())) {
                                ToxSaveStatus.Ok -> ""
                                ToxSaveStatus.BadProxyHost -> context.getString(R.string.bad_host)
                                ToxSaveStatus.BadProxyPort -> context.getString(R.string.bad_port)
                                ToxSaveStatus.BadProxyType -> context.getString(R.string.bad_type)
                                ToxSaveStatus.ProxyNotFound -> context.getString(R.string.proxy_not_found)
                                else -> context.getString(R.string.create_profile_error_failed)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.create_profile_btn), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

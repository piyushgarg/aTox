package ltd.evilcorp.atox.ui.navigation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.domain.features.call.CallManager
import ltd.evilcorp.domain.features.call.CallState
import ltd.evilcorp.domain.core.model.FINGERPRINT_LEN
import ltd.evilcorp.domain.core.model.PublicKey

@Composable
fun IncomingCallOverlay(
    callState: CallState,
    callManager: CallManager,
    notificationHelper: NotificationHelper,
) {
    val incomingCall = callState as? CallState.IncomingRinging ?: return
    val contact = incomingCall.contact
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.incoming_call)) },
        text = {
            Text(
                stringResource(
                    R.string.incoming_call_from,
                    contact.name.ifEmpty { contact.publicKey.take(FINGERPRINT_LEN) }
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        val pk = PublicKey(contact.publicKey)
                        if (callManager.acceptIncomingCall(pk)) {
                            notificationHelper.showOngoingCallNotification(contact)
                            notificationHelper.dismissCallNotification(pk)
                            callManager.startSendingAudio()
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        val pk = PublicKey(contact.publicKey)
                        callManager.endCall(pk)
                        notificationHelper.dismissCallNotification(pk)
                    }
                }
            ) {
                Text(stringResource(R.string.reject))
            }
        }
    )
}

package ltd.evilcorp.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.feature.ChatManager
import ltd.evilcorp.domain.feature.ContactManager
import ltd.evilcorp.domain.feature.FileTransferManager
import ltd.evilcorp.domain.feature.INotificationHelper

class DeleteContactUseCase @Inject constructor(
    private val callManager: CallManager,
    private val notificationHelper: INotificationHelper,
    private val contactManager: ContactManager,
    private val chatManager: ChatManager,
    private val fileTransferManager: FileTransferManager,
) {
    suspend fun execute(publicKey: PublicKey) = withContext(Dispatchers.IO) {
        callManager.endCall(publicKey)
        notificationHelper.dismissNotifications(publicKey)
        notificationHelper.dismissCallNotification(publicKey)
        contactManager.delete(publicKey).join()
        chatManager.clearHistory(publicKey)
        fileTransferManager.deleteAll(publicKey)
    }
}

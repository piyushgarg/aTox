package ltd.evilcorp.domain.features.contacts.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.call.CallManager
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.contacts.ContactManager
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.deleteAll
import ltd.evilcorp.domain.core.network.INotificationHelper

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
        contactManager.delete(publicKey)
        chatManager.clearHistory(publicKey)
        fileTransferManager.deleteAll(publicKey)
    }
}

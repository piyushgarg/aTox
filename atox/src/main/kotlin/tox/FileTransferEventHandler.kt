package ltd.evilcorp.atox.tox

import android.content.Context
import android.util.Log
import java.net.URLConnection
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FtAutoAccept
import ltd.evilcorp.core.model.FileKind
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.core.model.FINGERPRINT_LEN
import ltd.evilcorp.core.tox.enums.ToxFileControl
import ltd.evilcorp.domain.feature.ChatManager
import ltd.evilcorp.domain.feature.FileTransferManager
import ltd.evilcorp.domain.tox.ITox

private const val TAG = "FileTransferEventHandler"

private fun isImage(filename: String) = try {
    URLConnection.guessContentTypeFromName(filename).startsWith("image/")
} catch (e: Exception) {
    Log.e(TAG, e.toString())
    false
}

private fun String.fingerprint() = this.take(FINGERPRINT_LEN)

class FileTransferEventHandler @Inject constructor(
    private val scope: CoroutineScope,
    private val context: Context,
    private val contactRepository: ContactRepository,
    private val fileTransferManager: FileTransferManager,
    private val chatManager: ChatManager,
    private val notificationHelper: NotificationHelper,
    private val tox: ITox,
    private val settings: Settings,
) {
    private suspend fun tryGetContact(pk: String, tag: String) = contactRepository.get(pk).firstOrNull().let {
        if (it == null) {
            Log.e(TAG, "$tag -> unable to get contact for ${pk.fingerprint()}")
        }
        it
    }

    private fun notifyMessage(contact: Contact, message: String) =
        notificationHelper.showMessageNotification(contact, message, silent = tox.getStatus() == UserStatus.Busy)

    fun onFileRecvChunk(publicKey: String, fileNumber: Int, position: Long, data: ByteArray) {
        fileTransferManager.addDataToTransfer(publicKey, fileNumber, position, data)
    }

    fun onFileRecv(publicKey: String, fileNo: Int, kind: Int, fileSize: Long, filename: String) {
        val name = if (kind == FileKind.Avatar.ordinal) publicKey else filename
        val id = fileTransferManager.add(FileTransfer(publicKey, fileNo, kind, fileSize, name, outgoing = false))

        if (kind == FileKind.Data.ordinal) {
            if (chatManager.activeChat != publicKey) {
                scope.launch {
                    val contact = tryGetContact(publicKey, "FileRecv") ?: Contact(publicKey)
                    val message = context.getString(R.string.notification_file_transfer, name)
                    notifyMessage(contact, message)
                }
                contactRepository.setHasUnreadMessages(publicKey, true)
            }

            val autoAccept = settings.ftAutoAccept
            if (autoAccept == FtAutoAccept.All || (autoAccept == FtAutoAccept.Images && isImage(filename))) {
                fileTransferManager.accept(id)
            }
        }
    }

    fun onFileRecvControl(publicKey: String, fileNo: Int, control: ToxFileControl) {
        fileTransferManager.setStatus(publicKey, fileNo, control)
    }

    fun onFileChunkRequest(publicKey: String, fileNo: Int, position: Long, length: Int) {
        fileTransferManager.sendChunk(publicKey, fileNo, position, length)
    }
}

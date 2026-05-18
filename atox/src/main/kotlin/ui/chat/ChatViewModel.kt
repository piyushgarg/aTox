// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.squareup.picasso.Picasso
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.feature.CallState
import ltd.evilcorp.domain.feature.ChatManager
import ltd.evilcorp.domain.feature.ContactManager
import ltd.evilcorp.domain.feature.ExportManager
import ltd.evilcorp.domain.feature.FileTransferManager

private const val TAG = "ChatViewModel"

enum class CallAvailability {
    Unavailable,
    Available,
    Active,
}

class ChatViewModel @Inject constructor(
    private val callManager: CallManager,
    private val chatManager: ChatManager,
    private val exportManager: ExportManager,
    private val contactManager: ContactManager,
    private val fileTransferManager: FileTransferManager,
    private val notificationHelper: NotificationHelper,
    private val resolver: ContentResolver,
    private val context: Context,
    private val scope: CoroutineScope,
    private val settings: Settings,
) : ViewModel() {
    private var publicKey = PublicKey("")
    private var sentTyping = false

    private val activePublicKey = MutableStateFlow<PublicKey?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val contact: LiveData<Contact?> = activePublicKey
        .filterNotNull()
        .flatMapLatest { pk -> contactManager.get(pk) }
        .asLiveData()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: LiveData<List<Message>> = activePublicKey
        .filterNotNull()
        .flatMapLatest { pk -> chatManager.messagesFor(pk) }
        .distinctUntilChanged()
        .asLiveData()

    @OptIn(ExperimentalCoroutinesApi::class)
    val fileTransfers: LiveData<List<FileTransfer>> = activePublicKey
        .filterNotNull()
        .flatMapLatest { pk -> fileTransferManager.transfersFor(pk) }
        .asLiveData()

    fun callingNeedsConfirmation(): Boolean = settings.confirmCalling
    val ongoingCall = callManager.inCall.asLiveData()

    @OptIn(ExperimentalCoroutinesApi::class)
    val callState: LiveData<CallAvailability> = activePublicKey
        .filterNotNull()
        .flatMapLatest { pk ->
            contactManager.get(pk)
                .filterNotNull()
                .transform { emit(it.connectionStatus != ConnectionStatus.None) }
                .combine(callManager.inCall) { contactOnline, callState ->
                    if (!contactOnline) return@combine CallAvailability.Unavailable
                    when (callState) {
                        CallState.NotInCall -> CallAvailability.Available
                        is CallState.InCall -> {
                            if (callState.publicKey == pk) {
                                CallAvailability.Active
                            } else {
                                CallAvailability.Unavailable
                            }
                        }
                    }
                }
        }.asLiveData()

    var contactOnline = false

    fun send(message: String, type: MessageType) = chatManager.sendMessage(publicKey, message, type)

    fun clearHistory() = scope.launch {
        chatManager.clearHistory(publicKey)
        fileTransferManager.deleteAll(publicKey)
    }

    fun setActiveChat(pk: PublicKey) {
        if (pk.string().isEmpty()) {
            Log.i(TAG, "Clearing active chat")
            setTyping(false)
            activePublicKey.value = null
        } else {
            Log.i(TAG, "Setting active chat ${pk.fingerprint()}")
            activePublicKey.value = pk
        }

        publicKey = pk
        notificationHelper.dismissNotifications(publicKey)
        chatManager.activeChat = publicKey.string()
    }

    fun setTyping(typing: Boolean) {
        if (publicKey.string().isEmpty()) return
        if (sentTyping != typing) {
            chatManager.setTyping(publicKey, typing)
            sentTyping = typing
        }
    }

    fun acceptFt(id: Int) = scope.launch {
        fileTransferManager.accept(id)
    }

    fun rejectFt(id: Int) = scope.launch {
        fileTransferManager.reject(id)
    }

    fun createFt(file: Uri) = scope.launch {
        // Make sure there's no stale cached image in Picasso.
        // This happens if the user sends 2 different files with the same path (e.g. by overwriting one with the other.)
        Picasso.get().invalidate(file)
        fileTransferManager.create(publicKey, file)
    }

    fun delete(msg: Message) = scope.launch {
        if (msg.type == MessageType.FileTransfer) {
            fileTransferManager.delete(msg.correlationId)
        }
        chatManager.deleteMessage(msg.id)
    }

    fun exportFt(id: Int, dest: Uri) = scope.launch {
        fileTransferManager.get(id).take(1).collect { ft ->
            launch(Dispatchers.IO) {
                try {
                    resolver.openInputStream(ft.destination.toUri())?.use { ins ->
                        resolver.openOutputStream(dest).use { os ->
                            ins.copyTo(os!!)
                        }
                    } ?: throw IllegalStateException("Unable to open ${ft.destination}")
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.export_file_failure, Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.export_file_success, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun backupHistory(publicKey: String, locationSave: Uri) = scope.launch {
        val backupContent = exportManager.generateExportMessagesJString(publicKey)
        launch(Dispatchers.IO) {
            try {
                resolver.openOutputStream(locationSave).use { os ->
                    backupContent.byteInputStream().copyTo(os!!)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        R.string.export_history_success,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.export_history_failure, e.message),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    fun setDraft(draft: String) = contactManager.setDraft(publicKey, draft)
    fun clearDraft() = setDraft("")

    fun onEndCall() {
        callManager.endCall(publicKey)
        notificationHelper.dismissCallNotification(publicKey)
    }
}

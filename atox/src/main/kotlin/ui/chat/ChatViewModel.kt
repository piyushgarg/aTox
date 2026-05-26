// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.map
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
    private val fileExporter: FileExporter,
    private val settings: Settings,
) : ViewModel(), IChatController {
    private var publicKey = PublicKey("")
    private var sentTyping = false

    private val activePublicKey = MutableStateFlow<PublicKey?>(null)
    private val contentPublicKey = MutableStateFlow<PublicKey?>(null)
    private var contentActivationJob: Job? = null

    val replyingToMessage = MutableStateFlow<Message?>(null)

    fun setReplyingTo(message: Message?) {
        replyingToMessage.value = message
    }

    sealed interface ChatUiEvent {
        data class ShowToast(val messageResId: Int, val formatArg: String? = null) : ChatUiEvent
    }

    private val _uiEvents = MutableSharedFlow<ChatUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val contact: StateFlow<Contact?> = activePublicKey
        .filterNotNull()
        .flatMapLatest { pk -> contactManager.get(pk) }
        
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val isTyping: StateFlow<Boolean> = contact
        .map { it?.typing == true }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _fileTransfers = MutableStateFlow<List<FileTransfer>>(emptyList())
    val fileTransfers: StateFlow<List<FileTransfer>> = _fileTransfers.asStateFlow()

    fun callingNeedsConfirmation(): Boolean = settings.confirmCalling
    val ongoingCall = callManager.inCall

    @OptIn(ExperimentalCoroutinesApi::class)
    val callState: StateFlow<CallAvailability> = activePublicKey
        .filterNotNull()
        .flatMapLatest { pk ->
            contactManager.get(pk)
                .filterNotNull()
                .transform { emit(it.connectionStatus != ConnectionStatus.None) }
                .combine(callManager.inCall) { contactOnline, callState ->
                    if (!contactOnline) return@combine CallAvailability.Unavailable
                    when (callState) {
                        CallState.Idle -> CallAvailability.Available
                        is CallState.IncomingRinging ->
                            if (callState.contact.publicKey == pk.string()) CallAvailability.Active else CallAvailability.Unavailable
                        is CallState.OutgoingRequesting ->
                            if (callState.publicKey == pk) CallAvailability.Active else CallAvailability.Unavailable
                        is CallState.OutgoingWaiting ->
                            if (callState.publicKey == pk) CallAvailability.Active else CallAvailability.Unavailable
                        is CallState.Connecting ->
                            if (callState.publicKey == pk) CallAvailability.Active else CallAvailability.Unavailable
                        is CallState.OutgoingRinging ->
                            if (callState.publicKey == pk) CallAvailability.Active else CallAvailability.Unavailable
                        is CallState.Active ->
                            if (callState.publicKey == pk) CallAvailability.Active else CallAvailability.Unavailable
                    }
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CallAvailability.Unavailable
        )

    var contactOnline = false

    fun send(message: String, type: MessageType) {
        val replyMsg = replyingToMessage.value
        val textToSend = if (replyMsg != null && settings.enableReplies) {
            "[reply:${replyMsg.message.hashCode()}] $message"
        } else {
            message
        }
        replyingToMessage.value = null
        chatManager.sendMessage(publicKey, textToSend, type)
    }

    fun startCall() = viewModelScope.launch {
        if (callManager.startOutgoingCall(publicKey)) {
            callManager.startSendingAudio()
            notificationHelper.showOngoingCallNotification(contactManager.get(publicKey).take(1).first() ?: Contact(publicKey.string()))
        }
    }

    fun clearHistory() = viewModelScope.launch {
        chatManager.clearHistory(publicKey)
        fileTransferManager.deleteAll(publicKey)
    }

    fun setActiveChat(pk: PublicKey) {
        if (pk.string().isEmpty()) {
            Log.i(TAG, "Clearing active chat")
            setTyping(false)
            activePublicKey.value = null
            contentActivationJob?.cancel()
            contentActivationJob = null
            contentPublicKey.value = null
            _messages.value = emptyList()
            _fileTransfers.value = emptyList()
        } else {
            Log.i(TAG, "Setting active chat ${pk.fingerprint()}")
            activePublicKey.value = pk
            _messages.value = ChatHistoryCache.get(pk.string())
            _fileTransfers.value = ChatHistoryCache.getTransfers(pk.string())
            contentActivationJob?.cancel()
            contentPublicKey.value = null
            contentActivationJob = viewModelScope.launch {
                delay(450L)
                if (activePublicKey.value == pk) {
                    contentPublicKey.value = pk
                    
                    launch {
                        chatManager.messagesFor(pk).collect { list ->
                            _messages.value = list
                            ChatHistoryCache.put(pk.string(), list.takeLast(15))
                        }
                    }
                    
                    launch {
                        fileTransferManager.transfersFor(pk).collect { list ->
                            _fileTransfers.value = list
                            ChatHistoryCache.putTransfers(pk.string(), list)
                        }
                    }
                }
            }
        }

        publicKey = pk
        applyActiveChatSideEffects(pk)
    }

    private fun applyActiveChatSideEffects(pk: PublicKey) {
        notificationHelper.dismissNotifications(pk)
        chatManager.activeChat = pk.string()
    }

    fun clearActiveChat(expectedPublicKey: PublicKey) {
        if (publicKey == expectedPublicKey) {
            viewModelScope.launch {
                delay(450L)
                if (publicKey == expectedPublicKey) {
                    setActiveChat(PublicKey(""))
                }
            }
        }
    }

    fun setTyping(typing: Boolean) {
        if (publicKey.string().isEmpty()) return
        if (sentTyping != typing) {
            chatManager.setTyping(publicKey, typing)
            sentTyping = typing
        }
    }

    fun acceptFt(id: Int) = viewModelScope.launch {
        fileTransferManager.accept(id)
    }

    fun rejectFt(id: Int) = viewModelScope.launch {
        fileTransferManager.reject(id)
    }

    fun createFt(file: Uri) = viewModelScope.launch {
        // Make sure there's no stale cached image in Picasso via NotificationHelper
        notificationHelper.invalidateAvatar(file)
        fileTransferManager.create(publicKey, file)
    }

    fun delete(msg: Message) = viewModelScope.launch {
        if (msg.type == MessageType.FileTransfer) {
            fileTransferManager.delete(msg.correlationId)
        }
        chatManager.deleteMessage(msg.id)
    }

    fun exportFt(id: Int, dest: Uri) = viewModelScope.launch {
        fileTransferManager.get(id).take(1).collect { ft ->
            val result = fileExporter.exportFile(ft.destination, dest.toString())
            if (result.isSuccess) {
                _uiEvents.emit(ChatUiEvent.ShowToast(R.string.export_file_success))
            } else {
                _uiEvents.emit(ChatUiEvent.ShowToast(R.string.export_file_failure))
            }
        }
    }

    fun backupHistory(publicKey: String, locationSave: Uri) = viewModelScope.launch {
        val backupContent = exportManager.generateExportMessagesJString(publicKey)
        val result = fileExporter.exportHistory(backupContent, locationSave.toString())
        if (result.isSuccess) {
            _uiEvents.emit(ChatUiEvent.ShowToast(R.string.export_history_success))
        } else {
            val errorMsg = result.exceptionOrNull()?.message
            _uiEvents.emit(ChatUiEvent.ShowToast(R.string.export_history_failure, errorMsg))
        }
    }

    fun setDraft(draft: String) = contactManager.setDraft(publicKey, draft)
    fun clearDraft() = setDraft("")

    fun onEndCall() {
        callManager.endCall(publicKey)
        notificationHelper.dismissCallNotification(publicKey)
    }

    override fun sendMessage(message: String, type: MessageType) {
        send(message, type)
    }

    override fun sendFile(uri: Uri) {
        createFt(uri)
    }

    override fun sendVoice(uri: Uri) {
        createFt(uri)
    }

    override fun acceptFileTransfer(id: Int) {
        acceptFt(id)
    }

    override fun rejectFileTransfer(id: Int) {
        rejectFt(id)
    }

    override fun setDraftMessage(draft: String) {
        setDraft(draft)
    }
}

// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.contacts.ContactManager
import ltd.evilcorp.domain.core.network.INotificationHelper
import ltd.evilcorp.domain.features.chat.usecase.SendChatMessageUseCase
import ltd.evilcorp.atox.ui.common.debounceOffline

private const val TAG = "ChatViewModel"

enum class CallAvailability {
    Unavailable,
    Available,
    Active,
}

/**
 * Chat and file transfer management controller.
 * Designed with a fully declarative reactive flow pipeline using [flatMapLatest]
 * to prevent coroutine subscription leaks when active sessions or chats change.
 */
private const val CLEAR_ACTIVE_CHAT_DELAY_MS = 450L

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatCallDelegate: ChatCallDelegate,
    private val chatFileTransferDelegate: ChatFileTransferDelegate,
    private val chatManager: ChatManager,
    private val contactManager: ContactManager,
    private val notificationHelper: INotificationHelper,
    private val settings: Settings,
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    val voiceRecorder: ltd.evilcorp.domain.features.call.service.IVoiceRecorder,
) : ViewModel(), IChatController {
    private var publicKey = PublicKey("")
    private var sentTyping = false

    private val activePublicKey = MutableStateFlow<PublicKey?>(null)

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
        .flatMapLatest { pk -> contactManager.get(pk).debounceOffline() }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<Message>> = activePublicKey
        .flatMapLatest { pk ->
            if (pk == null || pk.string().isEmpty()) {
                flowOf(emptyList())
            } else {
                chatManager.messagesFor(pk)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val fileTransfers: StateFlow<List<FileTransfer>> = chatFileTransferDelegate.transfersFor(activePublicKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun callingNeedsConfirmation(): Boolean = settings.confirmCalling
    val ongoingCall = chatCallDelegate.ongoingCall

    val uiConfig: StateFlow<ChatUiConfig> = settings.state
        .map { userSettings ->
            ChatUiConfig(
                hapticEnabled = userSettings.hapticEnabled,
                dateFormatPreference = userSettings.dateFormatPreference,
                timeFormatPreference = userSettings.timeFormatPreference,
                sentMessageSoundUri = userSettings.sentMessageSoundUri,
                sentMessageSoundVolume = userSettings.sentMessageSoundVolume,
                enableReplies = userSettings.enableReplies,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatUiConfig(
                hapticEnabled = settings.hapticEnabled,
                dateFormatPreference = settings.dateFormatPreference,
                timeFormatPreference = settings.timeFormatPreference,
                sentMessageSoundUri = settings.sentMessageSoundUri,
                sentMessageSoundVolume = settings.sentMessageSoundVolume,
                enableReplies = settings.enableReplies,
            )
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val callState: StateFlow<CallAvailability> = chatCallDelegate.getCallState(activePublicKey)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CallAvailability.Unavailable
        )

    val uiState: StateFlow<ChatUiState> = combine(
        contact,
        messages,
        fileTransfers,
        isTyping,
        callState,
        uiConfig,
        replyingToMessage
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        ChatUiState(
            contact = array[0] as? Contact,
            messages = array[1] as List<Message>,
            fileTransfers = array[2] as List<FileTransfer>,
            isTyping = array[3] as Boolean,
            callState = array[4] as CallAvailability,
            uiConfig = array[5] as? ChatUiConfig,
            replyingToMessage = array[6] as? Message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
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
        viewModelScope.launch {
            sendChatMessageUseCase.execute(publicKey, textToSend, type)
        }
    }

    fun startCall() = viewModelScope.launch {
        chatCallDelegate.startCall(publicKey)
    }

    fun clearHistory() = viewModelScope.launch {
        chatManager.clearHistory(publicKey)
        chatFileTransferDelegate.clearTransfers(publicKey)
    }

    fun setActiveChat(pk: PublicKey) {
        if (pk.string().isEmpty()) {
            Log.i(TAG, "Clearing active chat")
            setTyping(false)
            activePublicKey.value = null
        } else {
            Log.i(TAG, "Setting active chat ${pk.fingerprint()}")
            // Atomically reset the current state when changing the chat to avoid flicker
            activePublicKey.value = null
            replyingToMessage.value = null
            activePublicKey.value = pk
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
                delay(CLEAR_ACTIVE_CHAT_DELAY_MS)
                if (publicKey == expectedPublicKey) {
                    setActiveChat(PublicKey(""))
                }
            }
        }
    }

    fun setTyping(typing: Boolean) {
        if (publicKey.string().isEmpty()) return
        if (sentTyping != typing) {
            viewModelScope.launch {
                chatManager.setTyping(publicKey, typing)
            }
            sentTyping = typing
        }
    }

    fun acceptFt(id: Int) = viewModelScope.launch {
        chatFileTransferDelegate.acceptFt(id)
    }

    fun rejectFt(id: Int) = viewModelScope.launch {
        chatFileTransferDelegate.rejectFt(id)
    }

    fun createFt(file: Uri) = viewModelScope.launch {
        chatFileTransferDelegate.createFt(publicKey, file)
    }

    fun delete(msg: Message) = viewModelScope.launch {
        if (msg.type == MessageType.FileTransfer) {
            chatFileTransferDelegate.deleteFt(msg.correlationId)
        }
        chatManager.deleteMessage(msg.id)
    }

    fun exportFt(id: Int, dest: Uri) = viewModelScope.launch {
        val result = chatFileTransferDelegate.exportFt(id, dest)
        if (result.isSuccess) {
            _uiEvents.emit(ChatUiEvent.ShowToast(R.string.export_file_success))
        } else {
            _uiEvents.emit(ChatUiEvent.ShowToast(R.string.export_file_failure))
        }
    }

    fun exportHistory(dest: Uri) = viewModelScope.launch {
        val result = chatFileTransferDelegate.exportHistory(publicKey, dest)
        if (result.isSuccess) {
            _uiEvents.emit(ChatUiEvent.ShowToast(R.string.export_history_success))
        } else {
            val errorMsg = result.exceptionOrNull()?.message
            _uiEvents.emit(ChatUiEvent.ShowToast(R.string.export_history_failure, errorMsg))
        }
    }

    fun setDraft(draft: String) {
        viewModelScope.launch {
            contactManager.setDraft(publicKey, draft)
        }
    }

    fun clearDraft() = setDraft("")

    fun onEndCall() {
        chatCallDelegate.endCall(publicKey)
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

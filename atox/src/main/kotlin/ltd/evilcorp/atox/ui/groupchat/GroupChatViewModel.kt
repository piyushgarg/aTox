package ltd.evilcorp.atox.ui.groupchat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.usecase.GetGroupChatUseCase
import ltd.evilcorp.domain.features.group.usecase.SetActiveGroupUseCase
import ltd.evilcorp.domain.features.group.usecase.SyncGroupMetadataUseCase
import ltd.evilcorp.domain.features.group.usecase.GetGroupConnectionStatusUseCase
import ltd.evilcorp.domain.features.group.usecase.GetGroupMessagesUseCase
import ltd.evilcorp.domain.features.group.usecase.GetGroupPeersUseCase
import ltd.evilcorp.domain.features.group.usecase.GetGroupFileTransfersUseCase
import ltd.evilcorp.domain.features.contacts.usecase.GetContactsUseCase
import ltd.evilcorp.domain.features.auth.usecase.GetSelfAvatarUriUseCase
import ltd.evilcorp.domain.features.group.usecase.GroupChatActions
import ltd.evilcorp.domain.features.group.usecase.GroupFileTransferActions
import ltd.evilcorp.domain.features.group.usecase.LeaveGroupUseCase
import ltd.evilcorp.domain.features.group.usecase.InviteFriendToGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel

private const val METADATA_SYNC_DELAY_MS = 3000L

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    private val getGroupChatUseCase: GetGroupChatUseCase,
    private val setActiveGroupUseCase: SetActiveGroupUseCase,
    private val syncGroupMetadataUseCase: SyncGroupMetadataUseCase,
    private val getGroupConnectionStatusUseCase: GetGroupConnectionStatusUseCase,
    private val getGroupMessagesUseCase: GetGroupMessagesUseCase,
    private val getGroupPeersUseCase: GetGroupPeersUseCase,
    private val getGroupFileTransfersUseCase: GetGroupFileTransfersUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val getSelfAvatarUriUseCase: GetSelfAvatarUriUseCase,
    private val chatActions: GroupChatActions,
    private val fileTransferActions: GroupFileTransferActions,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val inviteFriendToGroupUseCase: InviteFriendToGroupUseCase,
    val voiceRecorder: ltd.evilcorp.domain.features.call.service.IVoiceRecorder,
) : ViewModel(), ltd.evilcorp.atox.ui.chat.IChatController {
    private var chatId = ""
    private var metadataSyncJob: kotlinx.coroutines.Job? = null

    private val activeGroupChatId = MutableStateFlow<String?>(null)

    private val _selfAvatarUri = MutableStateFlow("")
    val selfAvatarUri: StateFlow<String> = _selfAvatarUri.asStateFlow()

    init {
        val uri = getSelfAvatarUriUseCase.execute()
        if (uri != null) {
            _selfAvatarUri.value = uri
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val group: StateFlow<Group?> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> getGroupChatUseCase.execute(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val connectionStatus: StateFlow<GroupConnectionStatus> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> getGroupConnectionStatusUseCase.execute(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupConnectionStatus.Disconnected)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<GroupMessage>> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> getGroupMessagesUseCase.execute(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val peers: StateFlow<List<GroupPeer>> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> getGroupPeersUseCase.execute(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<Contact>> = getContactsUseCase.execute()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val fileTransfers: StateFlow<List<FileTransfer>> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> getGroupFileTransfersUseCase.execute(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setActiveGroup(chatId: String) {
        this.chatId = chatId
        activeGroupChatId.value = chatId
        setActiveGroupUseCase.execute(chatId)
 
        // Start background synchronization of metadata and peer keys (avatars)
        metadataSyncJob?.cancel()
        metadataSyncJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                syncGroupMetadataUseCase.execute(chatId)
                delay(METADATA_SYNC_DELAY_MS)
            }
        }
    }

    override fun sendMessage(message: String, type: MessageType) {
        if (message.trim().isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            chatActions.sendGroupMessage.execute(chatId, message, type)
        }
    }

    override fun sendFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            chatActions.sendGroupFile.execute(chatId, uri.toString())
        }
    }

    override fun sendVoice(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            chatActions.sendGroupVoice.execute(chatId, uri.toString())
        }
    }

    fun acceptFt(id: Int) {
        viewModelScope.launch {
            fileTransferActions.acceptGroupFileTransfer.execute(id)
        }
    }

    fun rejectFt(id: Int) {
        viewModelScope.launch {
            fileTransferActions.rejectGroupFileTransfer.execute(id)
        }
    }

    fun cancelFt(msg: GroupMessage) {
        viewModelScope.launch {
            fileTransferActions.cancelGroupFileTransfer.execute(msg.correlationId)
        }
    }

    fun saveFt(id: Int, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            fileTransferActions.saveGroupFileTransfer.execute(id, uri.toString())
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatActions.clearGroupHistory.execute(chatId)
        }
    }

    fun deleteMessage(msg: GroupMessage) {
        viewModelScope.launch {
            chatActions.deleteGroupMessage.execute(msg.id)
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            leaveGroupUseCase.execute(chatId)
        }
    }

    fun setDraft(draft: String) {
        viewModelScope.launch {
            chatActions.setGroupDraft.execute(chatId, draft)
        }
    }

    fun getChatId(): String? = chatId

    fun inviteFriend(friendPublicKey: String) {
        viewModelScope.launch {
            inviteFriendToGroupUseCase.execute(chatId, friendPublicKey)
        }
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

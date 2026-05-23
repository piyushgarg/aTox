package ltd.evilcorp.atox.ui.groupchat

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.media.SystemSoundPlayer
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupMessage
import ltd.evilcorp.core.model.GroupPeer
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.domain.feature.GroupManager

class GroupChatViewModel @Inject constructor(
    private val groupManager: GroupManager,
    private val contactRepository: ContactRepository,
    private val context: Context,
    private val settings: Settings,
    private val systemSoundPlayer: SystemSoundPlayer,
) : ViewModel() {
    private var chatId = ""

    private val activeGroupChatId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val group: LiveData<Group?> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> groupManager.get(cid) }
        .asLiveData()

    @OptIn(ExperimentalCoroutinesApi::class)
    val connectionStatus: LiveData<GroupConnectionStatus> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> groupManager.connectionStatuses }
        .asLiveData()
        .map { statuses -> statuses[chatId] ?: GroupConnectionStatus.Disconnected }

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: LiveData<List<GroupMessage>> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> groupManager.messagesFor(cid) }
        .asLiveData()

    @OptIn(ExperimentalCoroutinesApi::class)
    val peers: LiveData<List<GroupPeer>> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> groupManager.getPeers(cid) }
        .asLiveData()

    val contacts: LiveData<List<Contact>> = contactRepository.getAll().asLiveData()

    fun setActiveGroup(chatId: String) {
        this.chatId = chatId
        activeGroupChatId.value = chatId
        groupManager.activeGroup = chatId
    }

    fun sendMessage(message: String) {
        if (message.trim().isEmpty()) return
        groupManager.sendMessage(chatId, message, MessageType.Normal)
        systemSoundPlayer.playSentSound(settings.sentMessageSoundUri, settings.sentMessageSoundVolume)
    }

    fun clearHistory() {
        viewModelScope.launch {
            groupManager.clearHistory(chatId)
        }
    }

    fun deleteMessage(msg: GroupMessage) {
        viewModelScope.launch {
            groupManager.deleteMessage(msg.id)
        }
    }

    fun leaveGroup() {
        groupManager.leaveGroup(chatId)
    }

    fun setDraft(draft: String) {
        viewModelScope.launch {
            groupManager.setDraft(chatId, draft)
        }
    }

    fun getChatId(): String? = groupManager.getChatId(chatId)

    fun inviteFriend(friendPublicKey: String): Boolean = groupManager.inviteFriend(chatId, friendPublicKey)
}

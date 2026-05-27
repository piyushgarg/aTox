package ltd.evilcorp.atox.ui.groupchat

import android.content.Context
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.Group
import ltd.evilcorp.domain.model.GroupMessage
import ltd.evilcorp.domain.model.GroupPeer
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.Sender
import ltd.evilcorp.domain.model.FileTransfer
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.repository.GroupRepository
import ltd.evilcorp.core.repository.FileTransferRepository
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.domain.feature.GroupManager
import ltd.evilcorp.domain.feature.FileTransferManager
import java.io.File
import java.util.Date

import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    private val groupManager: GroupManager,
    private val contactRepository: ContactRepository,
    private val context: Context,
    private val systemSoundPlayer: SystemSoundPlayer,
    private val groupRepository: GroupRepository,
    private val fileTransferRepository: FileTransferRepository,
    private val fileTransferManager: FileTransferManager,
) : ViewModel(), ltd.evilcorp.atox.ui.chat.IChatController {
    private var chatId = ""
    private var metadataSyncJob: kotlinx.coroutines.Job? = null

    private val activeGroupChatId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            groupManager.groupMigratedEvent.collect { (oldId, newId) ->
                if (chatId == oldId) {
                    Log.i("GroupChatViewModel", "Active group migrated from $oldId to $newId, dynamic redirection triggered.")
                    setActiveGroup(newId)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val group: StateFlow<Group?> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> groupManager.get(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val connectionStatus: StateFlow<GroupConnectionStatus> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> groupManager.connectionStatuses }
        .map { statuses -> statuses[chatId] ?: GroupConnectionStatus.Disconnected }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupConnectionStatus.Disconnected)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<GroupMessage>> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> groupManager.messagesFor(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val peers: StateFlow<List<GroupPeer>> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> groupManager.getPeers(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<Contact>> = contactRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val fileTransfers: StateFlow<List<FileTransfer>> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> fileTransferRepository.get(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setActiveGroup(chatId: String) {
        this.chatId = chatId
        activeGroupChatId.value = chatId
        groupManager.activeGroup = chatId

        // Start background synchronization of metadata and peer keys (avatars)
        metadataSyncJob?.cancel()
        metadataSyncJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                groupManager.checkAndUpdateGroupMetadata(chatId)
                delay(3000)
            }
        }
    }

    fun sendMessageInternal(message: String, type: MessageType = MessageType.Normal, correlationId: Int = -1) {
        if (message.trim().isEmpty()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            groupManager.sendMessage(chatId, message, type)
            if (type == MessageType.FileTransfer && correlationId != -1) {
                delay(150)
                groupRepository.getMessages(chatId).take(1).collect { list ->
                    val lastMsg = list.lastOrNull { it.message == message }
                    if (lastMsg != null) {
                        lastMsg.correlationId = correlationId
                        groupRepository.addMessage(lastMsg)
                    }
                }
            }
        }
    }

    override fun sendMessage(message: String, type: MessageType) {
        sendMessageInternal(message, type, -1)
    }

    override fun sendFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val (name, size) = try {
                if (uri.scheme == "file") {
                    val f = File(uri.path ?: return@launch)
                    Pair(f.name, f.length())
                } else {
                    context.contentResolver.query(uri, null, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.SIZE))
                            val displayName = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                            Pair(displayName, fileSize)
                        } else null
                    }
                } ?: return@launch
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Failed to query file details", e)
                return@launch
            }

            val cacheDir = File(context.cacheDir, "outgoing")
            cacheDir.mkdirs()
            val destFile = File(cacheDir, "${java.util.UUID.randomUUID()}_$name")
            try {
                val input = if (uri.scheme == "file") {
                    java.io.FileInputStream(File(uri.path ?: throw java.io.FileNotFoundException()))
                } else {
                    context.contentResolver.openInputStream(uri)
                }
                input?.use { inp ->
                    destFile.outputStream().use { output ->
                        inp.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Failed to copy file to outgoing cache", e)
                return@launch
            }

            val correlationId = kotlin.random.Random.nextInt(1000000)
            val ft = FileTransfer(
                publicKey = chatId,
                fileNumber = correlationId,
                fileKind = ltd.evilcorp.domain.model.FileKind.Data.ordinal,
                fileSize = size,
                fileName = name,
                outgoing = true,
                progress = size,
                destination = Uri.fromFile(destFile).toString(),
            )
            val id = fileTransferRepository.add(ft).toInt()

            val signalMsg = "[FILE:$name|$size|$id]"
            sendMessageInternal(signalMsg, MessageType.FileTransfer, id)
        }
    }

    override fun sendVoice(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val f = File(uri.path ?: return@launch)
            val name = f.name
            val size = f.length()

            val correlationId = kotlin.random.Random.nextInt(1000000)
            val ft = FileTransfer(
                publicKey = chatId,
                fileNumber = correlationId,
                fileKind = ltd.evilcorp.domain.model.FileKind.Data.ordinal,
                fileSize = size,
                fileName = "voice_message_${correlationId}.m4a",
                outgoing = true,
                progress = size,
                destination = uri.toString(),
            )
            val id = fileTransferRepository.add(ft).toInt()

            val signalMsg = "[VOICE:10|$id]"
            sendMessageInternal(signalMsg, MessageType.FileTransfer, id)
        }
    }

    fun acceptFt(id: Int) {
        viewModelScope.launch {
            fileTransferManager.accept(id)
        }
    }

    fun rejectFt(id: Int) {
        viewModelScope.launch {
            fileTransferManager.reject(id)
        }
    }

    fun cancelFt(msg: GroupMessage) {
        viewModelScope.launch {
            fileTransferManager.delete(msg.correlationId)
        }
    }

    fun saveFt(id: Int, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            fileTransferRepository.get(id).take(1).collect { ft ->
                val sourceFile = File(Uri.parse(ft.destination).path ?: return@collect)
                if (sourceFile.exists()) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                }
            }
        }
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

    fun getChatId(): String? = chatId

    fun inviteFriend(friendPublicKey: String) {
        viewModelScope.launch {
            groupManager.inviteFriend(chatId, friendPublicKey)
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

package ltd.evilcorp.core.tox

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.transfer.model.FileKind

open class FakeTox(
    override val publicKey: PublicKey = PublicKey("OUR_PK")
) : ITox {
    override val toxId: ToxID get() = throw UnsupportedOperationException()
    override var nospam: Int get() = 0; set(_) {}
    override var started: Boolean = true
    override var isBootstrapNeeded: Boolean get() = false; set(_) {}
    override val password: String? get() = null
    override val sessionId: String? get() = "dummy_session"

    val reconnectedGroups = mutableListOf<Int>()
    val bootstrapFriendsAdded = mutableListOf<PublicKey>()
    val contactsDeleted = mutableListOf<PublicKey>()
    var chatList = intArrayOf()
    val joinDirectCalledWith = mutableListOf<Pair<ByteArray, String>>()
    val chatIdMock = mutableMapOf<Int, ByteArray>()
    val sentPackets = mutableListOf<Pair<PublicKey, ByteArray>>()

    override fun changePassword(new: String?) {}
    override fun stop() {}
    override fun getContacts(): List<Pair<PublicKey, Int>> = emptyList()
    override fun acceptFriendRequest(publicKey: PublicKey): Result<Unit> = Result.success(Unit)
    override fun startFileTransfer(pk: PublicKey, fileNumber: Int) {}
    override fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {}
    override fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int = 0
    override fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> = Result.success(Unit)
    override fun getName(): String = "Me"
    override fun setName(name: String) {}
    override fun getStatusMessage(): String = ""
    override fun setStatusMessage(statusMessage: String) {}
    override fun addContact(toxId: ToxID, message: String) {}
    override fun deleteContact(publicKey: PublicKey) {
        contactsDeleted.add(publicKey)
    }
    override fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int = 0
    override fun getSaveData(): ByteArray = byteArrayOf()
    override fun setTyping(publicKey: PublicKey, typing: Boolean): Boolean = true
    override fun friendGetTyping(publicKey: PublicKey): Boolean = false
    override fun getFriendNumber(publicKey: PublicKey): Int = 0
    override fun getFriendPublicKey(friendNumber: Int): PublicKey? = null
    override fun friendGetLastOnline(publicKey: PublicKey): Long = 0
    override fun getStatus(): UserStatus = UserStatus.None
    override fun setStatus(status: UserStatus) {}
    
    override fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): Boolean {
        sentPackets.add(Pair(pk, packet))
        return true
    }

    override fun startCall(pk: PublicKey): Boolean = true
    override fun answerCall(pk: PublicKey): Boolean = true
    override fun endCall(pk: PublicKey): Boolean = true
    override fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int): Boolean = true
    override fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int = 0
    override fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int = 0
    override fun groupLeave(groupNumber: Int): Boolean = true
    override fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int = 0
    override fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean = true
    override fun groupGetTopic(groupNumber: Int): ByteArray? = null
    override fun groupGetName(groupNumber: Int): ByteArray? = null
    override fun groupGetChatId(groupNumber: Int): ByteArray? = chatIdMock[groupNumber]
    override fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean = true
    override fun groupGetPassword(groupNumber: Int): ByteArray? = null
    override fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? = null
    override fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? = null
    override fun groupSelfGetPeerId(groupNumber: Int): Int = 0
    override fun groupSelfGetRole(groupNumber: Int): ToxGroupRole = ToxGroupRole.USER
    override fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean = true
    override fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int {
        joinDirectCalledWith.add(Pair(chatId, String(selfName)))
        return 0
    }
    override fun groupReconnect(groupNumber: Int): Boolean {
        reconnectedGroups.add(groupNumber)
        return true
    }
    override fun addFriendNoRequest(publicKey: PublicKey): Int = 0
    override fun groupGetChatlist(): IntArray = chatList
}

class FakeMessageRepository : IMessageRepository {
    private val messages = MutableStateFlow<List<Message>>(emptyList())

    override suspend fun add(message: Message) {
        messages.value = messages.value + message
    }

    override fun get(conversation: String): Flow<List<Message>> {
        return messages.map { list -> list.filter { it.publicKey == conversation } }
    }

    override suspend fun getPending(conversation: String): List<Message> {
        return messages.value.filter { it.publicKey == conversation && it.timestamp == 0L }
    }

    override suspend fun getPaged(conversation: String, limit: Int, offset: Int): List<Message> {
        val list = messages.value.filter { it.publicKey == conversation }.sortedByDescending { it.id }
        if (offset >= list.size) return emptyList()
        return list.drop(offset).take(limit)
    }

    override fun getPagingFlow(conversation: String): Flow<androidx.paging.PagingData<Message>> {
        return androidx.paging.Pager(
            config = androidx.paging.PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                object : androidx.paging.PagingSource<Int, Message>() {
                    override fun getRefreshKey(state: androidx.paging.PagingState<Int, Message>): Int? {
                        return state.anchorPosition
                    }

                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Message> {
                        val list = messages.value.filter { it.publicKey == conversation }.sortedByDescending { it.id }
                        val key = params.key ?: 0
                        val loadSize = params.loadSize
                        if (key >= list.size) {
                            return LoadResult.Page(
                                data = emptyList(),
                                prevKey = if (key > 0) key - loadSize else null,
                                nextKey = null
                            )
                        }
                        val data = list.drop(key).take(loadSize)
                        return LoadResult.Page(
                            data = data,
                            prevKey = if (key > 0) key - loadSize else null,
                            nextKey = if (key + loadSize < list.size) key + loadSize else null
                        )
                    }
                }
            }
        ).flow
    }

    override suspend fun setCorrelationId(id: Long, correlationId: Int) {
        messages.value = messages.value.map { msg ->
            if (msg.id == id) msg.copy(correlationId = correlationId) else msg
        }
    }

    override suspend fun delete(conversation: String) {
        messages.value = messages.value.filter { it.publicKey != conversation }
    }

    override suspend fun deleteMessage(id: Long) {
        messages.value = messages.value.filter { it.id != id }
    }

    override suspend fun setReceipt(conversation: String, correlationId: Int, timestamp: Long) {
        messages.value = messages.value.map { msg ->
            if (msg.publicKey == conversation && msg.correlationId == correlationId) {
                msg.copy(timestamp = timestamp)
            } else {
                msg
            }
        }
    }

    override suspend fun exists(conversation: String, message: String): Boolean {
        return messages.value.any { it.publicKey == conversation && it.message == message }
    }
}

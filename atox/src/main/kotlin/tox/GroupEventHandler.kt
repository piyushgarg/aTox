package ltd.evilcorp.atox.tox

import android.content.Context
import android.util.Log
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.media.SystemSoundPlayer
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.repository.GroupRepository
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupMessage
import ltd.evilcorp.core.model.GroupPeer
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.tox.enums.ToxGroupExitType
import ltd.evilcorp.core.tox.enums.ToxGroupJoinFail
import ltd.evilcorp.core.tox.enums.ToxGroupModEvent
import ltd.evilcorp.core.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.core.tox.enums.ToxGroupRole
import ltd.evilcorp.core.tox.enums.ToxMessageType
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.domain.feature.GroupInvite
import ltd.evilcorp.domain.feature.GroupManager
import ltd.evilcorp.domain.tox.Tox

private const val TAG = "GroupEventHandler"

class GroupEventHandler @Inject constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository,
    private val groupManager: GroupManager,
    private val notificationHelper: NotificationHelper,
    private val systemSoundPlayer: SystemSoundPlayer,
    private val tox: Tox,
    private val settings: Settings,
) {
    fun onGroupInvite(friendNo: Int, inviteData: ByteArray, groupName: String) {
        scope.launch {
            Log.i(TAG, "Group invite from friendNo=$friendNo: $groupName")
        }
        val invite = GroupInvite(friendNo = friendNo, inviteData = inviteData, groupName = groupName)
        groupManager.setPendingInvite(invite)
    }

    fun onGroupMessage(
        groupNo: Int,
        peerId: Int,
        type: ToxMessageType,
        message: String,
        messageId: Int,
    ) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch

            val peerNameBytes = tox.groupPeerGetName(groupNo, peerId)
            val peerName = peerNameBytes?.decodeToString() ?: "Unknown"

            val group = groupRepository.get(chatId).firstOrNull()
            if (group == null) {
                Log.e(TAG, "Group not found for chatId=$chatId")
                return@launch
            }

            val isOurPeer = peerId == group.selfPeerId

            if (groupRepository.existsByCorrelationId(chatId, messageId)) {
                return@launch
            }

            val groupMsg = GroupMessage(
                groupChatId = chatId,
                peerId = peerId,
                senderName = peerName,
                message = message,
                sender = if (isOurPeer) Sender.Sent else Sender.Received,
                type = type.toMessageType(),
                correlationId = messageId,
                timestamp = Date().time,
            )
            groupRepository.addMessage(groupMsg)

            if (!isOurPeer && groupManager.activeGroup != chatId) {
                systemSoundPlayer.playNotificationSound(settings.notificationSoundUri, settings.notificationSoundVolume)
                notificationHelper.showGroupMessageNotification(group.name, peerName, message)
                groupRepository.setHasUnreadMessages(chatId, true)
            } else {
                systemSoundPlayer.playNotificationSound(settings.activeChatSoundUri, settings.activeChatSoundVolume)
            }
        }
    }

    fun onGroupPeerJoin(groupNo: Int, peerId: Int) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch

            val group = groupRepository.get(chatId).firstOrNull() ?: return@launch

            val peerNameBytes = tox.groupPeerGetName(groupNo, peerId)
            val peerName = peerNameBytes?.decodeToString() ?: "Unknown"

            if (peerId != group.selfPeerId) {
                val peerKeyBytes = tox.groupPeerGetPublicKey(groupNo, peerId)
                val peerKey = peerKeyBytes?.toHexString() ?: ""

                val alreadyExistsByPubKey = peerKey.isNotEmpty() && groupRepository.peerExistsByPublicKey(chatId, peerKey)

                val isNewPeer = if (alreadyExistsByPubKey) {
                    groupRepository.deletePeerByPublicKey(chatId, peerKey)
                    false
                } else {
                    true
                }

                val peer = GroupPeer(
                    groupChatId = chatId,
                    peerId = peerId,
                    name = peerName,
                    publicKey = peerKey,
                )
                groupRepository.addPeer(peer)

                if (isNewPeer) {
                    val msg = GroupMessage(
                        groupChatId = chatId,
                        peerId = peerId,
                        senderName = peerName,
                        message = context.getString(R.string.group_peer_joined, peerName),
                        sender = Sender.Received,
                        type = MessageType.GroupEvent,
                        correlationId = 0,
                        timestamp = Date().time,
                    )
                    groupRepository.addMessage(msg)
                }
            }

            val count = groupRepository.peerCountDirect(chatId)
            groupRepository.setPeerCount(chatId, count)

            Log.i(TAG, "Peer joined group: $peerName ($peerId) in $chatId")
        }
    }

    fun onGroupPeerExit(groupNo: Int, peerId: Int, exitType: ToxGroupExitType) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch

            val group = groupRepository.get(chatId).firstOrNull() ?: return@launch

            val isSelf = peerId == group.selfPeerId

            if (isSelf) {
                if (exitType == ToxGroupExitType.SELF_DISCONNECTED) {
                    groupRepository.setConnected(chatId, false)
                    groupManager.setConnectionStatus(chatId, GroupConnectionStatus.Reconnecting)
                    Log.i(TAG, "Self disconnected from group $chatId (will reconnect)")
                    groupManager.scheduleAutoReconnect(chatId, group.groupNumber)
                }
                return@launch
            }

            if (exitType == ToxGroupExitType.DISCONNECTED || exitType == ToxGroupExitType.TIMEOUT) {
                groupManager.setConnectionStatus(chatId, GroupConnectionStatus.Connecting)
            }

            // For temporary disconnects (DISCONNECTED, TIMEOUT, SYNC_ERROR) keep the peer
            // in the DB to avoid inserting duplicate "joined" messages on reconnect.
            // Only remove and log permanent departures (QUIT, KICK).
            if (exitType == ToxGroupExitType.QUIT || exitType == ToxGroupExitType.KICK) {
                val peerName = groupRepository.getPeerNameDirect(chatId, peerId) ?: "Unknown"

                groupRepository.deletePeerById(chatId, peerId)

                val msgText = if (exitType == ToxGroupExitType.QUIT) {
                    context.getString(R.string.group_peer_left, peerName)
                } else {
                    context.getString(R.string.group_peer_kicked, peerName)
                }
                val msg = GroupMessage(
                    groupChatId = chatId,
                    peerId = peerId,
                    senderName = peerName,
                    message = msgText,
                    sender = Sender.Received,
                    type = MessageType.GroupEvent,
                    correlationId = 0,
                    timestamp = Date().time,
                )
                groupRepository.addMessage(msg)

                val count = groupRepository.peerCountDirect(chatId)
                groupRepository.setPeerCount(chatId, count)
            }

            Log.i(TAG, "Peer left group: peerId=$peerId, exitType=$exitType in $chatId")
        }
    }

    fun onGroupTopic(groupNo: Int, peerId: Int, topic: String) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            groupRepository.setTopic(chatId, topic)
            Log.i(TAG, "Group topic changed in $chatId")
        }
    }

    fun onGroupPeerName(groupNo: Int, peerId: Int, name: String) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            groupRepository.setPeerName(chatId, peerId, name)
        }
    }

    fun onGroupPassword(groupNo: Int, password: ByteArray) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            groupRepository.setPasswordProtected(chatId, password.isNotEmpty())
        }
    }

    fun onGroupPeerStatus(groupNo: Int, peerId: Int, status: ltd.evilcorp.core.tox.enums.ToxUserStatus) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            val userStatus = when (status) {
                ltd.evilcorp.core.tox.enums.ToxUserStatus.NONE -> ltd.evilcorp.core.model.UserStatus.None
                ltd.evilcorp.core.tox.enums.ToxUserStatus.AWAY -> ltd.evilcorp.core.model.UserStatus.Away
                ltd.evilcorp.core.tox.enums.ToxUserStatus.BUSY -> ltd.evilcorp.core.model.UserStatus.Busy
            }
            groupRepository.setPeerStatus(chatId, peerId, userStatus)
        }
    }

    fun onGroupPrivacyState(groupNo: Int, privacyState: ToxGroupPrivacyState) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            val state = when (privacyState) {
                ToxGroupPrivacyState.PUBLIC -> ltd.evilcorp.core.model.GroupPrivacyState.Public
                ToxGroupPrivacyState.PRIVATE -> ltd.evilcorp.core.model.GroupPrivacyState.Private
            }
            groupRepository.setPrivacyState(chatId, state)
        }
    }

    fun onGroupVoiceState(groupNo: Int, voiceState: ltd.evilcorp.core.tox.enums.ToxGroupVoiceState) {
        scope.launch {
            Log.i(TAG, "Group voice state changed in groupNo=$groupNo, state=$voiceState")
        }
    }

    fun onGroupTopicLock(groupNo: Int, topicLock: ltd.evilcorp.core.tox.enums.ToxGroupTopicLock) {
        scope.launch {
            Log.i(TAG, "Group topic lock changed in groupNo=$groupNo, lock=$topicLock")
        }
    }

    fun onGroupPeerLimit(groupNo: Int, peerLimit: Int) {
        scope.launch {
            Log.i(TAG, "Group peer limit changed in groupNo=$groupNo, limit=$peerLimit")
        }
    }

    fun onGroupPrivateMessage(groupNo: Int, peerId: Int, type: ToxMessageType, message: String) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            val peerNameBytes = tox.groupPeerGetName(groupNo, peerId)
            val peerName = peerNameBytes?.decodeToString() ?: "Unknown"
            Log.i(TAG, "Private message in group $chatId from $peerName: $message")
        }
    }

    fun onGroupConnected(groupNo: Int) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch
            groupRepository.setConnected(chatId, true)
            groupRepository.setGroupNumber(chatId, groupNo)
            groupManager.setConnectionStatus(chatId, GroupConnectionStatus.Connected)

            val groupNameBytes = tox.groupGetName(groupNo)
            val groupName = groupNameBytes?.decodeToString()
            if (!groupName.isNullOrBlank()) {
                groupRepository.setName(chatId, groupName)
            }

            groupManager.resendPendingMessages(chatId)

            Log.i(TAG, "Connected to group $chatId")
        }
    }

    fun onGroupJoinFail(groupNo: Int, joinFail: ToxGroupJoinFail) {
        scope.launch {
            Log.e(TAG, "Failed to join groupNo=$groupNo, reason=$joinFail")
            val chatId = groupRepository.findChatIdByGroupNumber(groupNo)
            if (chatId != null) {
                groupManager.setConnectionStatus(chatId, GroupConnectionStatus.Disconnected)
                groupRepository.deleteAllPeers(chatId)
                groupRepository.deleteByChatId(chatId)
            }
            if (joinFail == ToxGroupJoinFail.INVALID_PASSWORD) {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.group_password_required),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    fun onGroupModeration(groupNo: Int, sourcePeerId: Int, targetPeerId: Int, modEvent: ToxGroupModEvent) {
        scope.launch {
            val chatIdBytes = tox.groupGetChatId(groupNo)
            val chatId = chatIdBytes?.toHexString() ?: return@launch

            val sourceNameBytes = tox.groupPeerGetName(groupNo, sourcePeerId)
            val sourceName = sourceNameBytes?.decodeToString() ?: "Unknown"

            Log.i(TAG, "Moderation in $chatId: $sourceName performed $modEvent on peer $targetPeerId")

            if (modEvent == ToxGroupModEvent.KICK || modEvent == ToxGroupModEvent.MODERATOR || modEvent == ToxGroupModEvent.USER) {
                val role = when (modEvent) {
                    ToxGroupModEvent.MODERATOR -> ToxGroupRole.MODERATOR.name
                    ToxGroupModEvent.USER -> ToxGroupRole.USER.name
                    else -> null
                }
                if (role != null) {
                    groupRepository.setPeerRole(chatId, targetPeerId, role)
                }
            }
        }
    }

    private fun ToxMessageType.toMessageType(): MessageType = when (this) {
        ToxMessageType.NORMAL -> MessageType.Normal
        ToxMessageType.ACTION -> MessageType.Action
        else -> MessageType.Normal
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}

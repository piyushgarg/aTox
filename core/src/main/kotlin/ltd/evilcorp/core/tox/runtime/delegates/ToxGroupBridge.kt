package ltd.evilcorp.core.tox.runtime.delegates

import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.core.tox.NativeToxAv
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import ltd.evilcorp.domain.core.network.enums.ToxMessageType

class ToxGroupBridge(
    private val nativeTox: NativeTox,
    private val nativeToxAv: NativeToxAv,
    lock: Any,
    toxPtrProvider: () -> Long,
) : BaseToxBridge(lock, toxPtrProvider) {
    fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int = withTox { ptr ->
        nativeTox.toxGroupNew(ptr, privacyState.value, groupName, selfName)
    }

    fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int = withTox { ptr ->
        nativeTox.toxGroupJoin(ptr, friendNo, inviteData, selfName, password)
    }

    fun groupLeave(groupNumber: Int): Boolean = withTox { ptr ->
        nativeTox.toxGroupLeave(ptr, groupNumber)
    }

    fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int = withTox { ptr ->
        nativeTox.toxGroupSendMessage(ptr, groupNumber, type.ordinal, message)
    }

    fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean = withTox { ptr ->
        nativeTox.toxGroupSetTopic(ptr, groupNumber, topic)
    }

    fun groupGetTopic(groupNumber: Int): ByteArray? = withTox { ptr ->
        nativeTox.toxGroupGetTopic(ptr, groupNumber)
    }

    fun groupGetName(groupNumber: Int): ByteArray? = withTox { ptr ->
        nativeTox.toxGroupGetName(ptr, groupNumber)
    }

    fun groupGetChatId(groupNumber: Int): ByteArray? = withTox { ptr ->
        nativeTox.toxGroupGetChatId(ptr, groupNumber)
    }

    fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean = withTox { ptr ->
        nativeTox.toxGroupSetPassword(ptr, groupNumber, password)
    }

    fun groupGetPassword(groupNumber: Int): ByteArray? = withTox { ptr ->
        nativeTox.toxGroupGetPassword(ptr, groupNumber)
    }

    fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? = withTox { ptr ->
        nativeTox.toxGroupPeerGetName(ptr, groupNumber, peerId)
    }

    fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? = withTox { ptr ->
        nativeTox.toxGroupPeerGetPublicKey(ptr, groupNumber, peerId)
    }

    fun groupSelfGetPeerId(groupNumber: Int): Int = withTox { ptr ->
        nativeTox.toxGroupSelfGetPeerId(ptr, groupNumber)
    }

    fun groupSelfGetRole(groupNumber: Int): ToxGroupRole = withTox { ptr ->
        ToxGroupRole.fromInt(nativeTox.toxGroupSelfGetRole(ptr, groupNumber))
    }

    fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean = withTox { ptr ->
        nativeTox.toxGroupInviteSend(ptr, groupNumber, friendNumber)
    }

    fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int = withTox { ptr ->
        nativeTox.toxGroupJoinDirect(ptr, chatId, selfName, password)
    }

    fun groupReconnect(groupNumber: Int): Boolean = withTox { ptr ->
        nativeTox.toxGroupReconnect(ptr, groupNumber)
    }

    fun groupGetChatlist(): IntArray = withTox { ptr ->
        nativeTox.toxGroupGetChatlist(ptr)
    }

    fun groupavAdd(): Int = withTox { ptr ->
        nativeToxAv.toxavAddAvGroupchat(ptr)
    }

    fun groupavJoin(groupNumber: Int): Int = withTox { ptr ->
        nativeToxAv.toxavJoinAvGroupchat(ptr, groupNumber)
    }

    fun groupavSendAudio(groupNumber: Int, pcm: ShortArray, channels: Int, samplingRate: Int): Int = withTox { ptr ->
        nativeToxAv.toxavGroupSendAudio(ptr, groupNumber, pcm, pcm.size, channels, samplingRate)
    }

    fun groupavEnableAudio(groupNumber: Int): Int = withTox { ptr ->
        nativeToxAv.toxavGroupchatEnableAv(ptr, groupNumber)
    }

    fun groupavDisableAudio(groupNumber: Int): Int = withTox { ptr ->
        nativeToxAv.toxavGroupchatDisableAv(ptr, groupNumber)
    }

    fun groupavIsEnabled(groupNumber: Int): Boolean = withTox { ptr ->
        nativeToxAv.toxavGroupchatAvEnabled(ptr, groupNumber)
    }
}

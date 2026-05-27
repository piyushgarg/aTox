package ltd.evilcorp.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import ltd.evilcorp.domain.model.GroupPeer
import ltd.evilcorp.domain.model.UserStatus

@Entity(
    tableName = "group_peers",
    primaryKeys = ["group_chat_id", "peer_id"]
)
data class GroupPeerEntity(
    @ColumnInfo(name = "group_chat_id")
    val groupChatId: String,

    @ColumnInfo(name = "peer_id")
    val peerId: Int,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "public_key")
    var publicKey: String = "",

    @ColumnInfo(name = "role")
    var role: String = "User",

    @ColumnInfo(name = "is_ourselves")
    var isOurselves: Boolean = false,

    @ColumnInfo(name = "status")
    var status: UserStatus = UserStatus.None,
) {
    fun toDomain(): GroupPeer = GroupPeer(
        groupChatId = groupChatId,
        peerId = peerId,
        name = name,
        publicKey = publicKey,
        role = role,
        isOurselves = isOurselves,
        status = status
    )

    companion object {
        fun fromDomain(groupPeer: GroupPeer): GroupPeerEntity = GroupPeerEntity(
            groupChatId = groupPeer.groupChatId,
            peerId = groupPeer.peerId,
            name = groupPeer.name,
            publicKey = groupPeer.publicKey,
            role = groupPeer.role,
            isOurselves = groupPeer.isOurselves,
            status = groupPeer.status
        )
    }
}

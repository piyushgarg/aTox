package ltd.evilcorp.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "group_peers",
    primaryKeys = ["group_chat_id", "peer_id"]
)
data class GroupPeer(
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
)

package ltd.evilcorp.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ltd.evilcorp.domain.model.FriendRequest

@Entity(tableName = "friend_requests")
data class FriendRequestEntity(
    @PrimaryKey
    @ColumnInfo(name = "public_key")
    val publicKey: String,

    @ColumnInfo(name = "message")
    val message: String = "",
) {
    fun toDomain(): FriendRequest = FriendRequest(
        publicKey = publicKey,
        message = message
    )

    companion object {
        fun fromDomain(friendRequest: FriendRequest): FriendRequestEntity = FriendRequestEntity(
            publicKey = friendRequest.publicKey,
            message = friendRequest.message
        )
    }
}

package ltd.evilcorp.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.UserStatus

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "public_key")
    val publicKey: String,

    @ColumnInfo(name = "name")
    var name: String = "aTox user",

    @ColumnInfo(name = "status_message")
    var statusMessage: String = "Brought to you live, by aTox",

    @ColumnInfo(name = "status")
    var status: UserStatus = UserStatus.None,

    @ColumnInfo(name = "connection_status")
    var connectionStatus: ConnectionStatus = ConnectionStatus.None,

    @ColumnInfo(name = "password")
    var password: String = "",
) {
    fun toDomain(): User = User(
        publicKey = publicKey,
        name = name,
        statusMessage = statusMessage,
        status = status,
        connectionStatus = connectionStatus,
        password = password
    )

    companion object {
        fun fromDomain(user: User): UserEntity = UserEntity(
            publicKey = user.publicKey,
            name = user.name,
            statusMessage = user.statusMessage,
            status = user.status,
            connectionStatus = user.connectionStatus,
            password = user.password
        )
    }
}

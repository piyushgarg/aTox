package ltd.evilcorp.core.db

import androidx.room.TypeConverter
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.model.UserStatus

class Converters private constructor() {
    companion object {
        @TypeConverter
        @JvmStatic
        fun toStatus(statusId: Int): UserStatus = UserStatus.fromId(statusId)

        @TypeConverter
        @JvmStatic
        fun fromStatus(status: UserStatus): Int = status.id

        @TypeConverter
        @JvmStatic
        fun toConnection(connectionId: Int): ConnectionStatus = ConnectionStatus.fromId(connectionId)

        @TypeConverter
        @JvmStatic
        fun fromConnection(connection: ConnectionStatus): Int = connection.id

        @TypeConverter
        @JvmStatic
        fun toSender(senderId: Int): Sender = Sender.fromId(senderId)

        @TypeConverter
        @JvmStatic
        fun fromSender(sender: Sender): Int = sender.id

        @TypeConverter
        @JvmStatic
        fun toMessageType(typeId: Int): MessageType = MessageType.fromId(typeId)

        @TypeConverter
        @JvmStatic
        fun fromMessageType(type: MessageType): Int = type.id
    }
}

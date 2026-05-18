package ltd.evilcorp.core.db

import androidx.room.TypeConverter
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.model.UserStatus

class Converters private constructor() {
    companion object {
        @TypeConverter
        @JvmStatic
        fun toStatus(status: Int): UserStatus = UserStatus.entries[status]

        @TypeConverter
        @JvmStatic
        fun fromStatus(status: UserStatus): Int = status.ordinal

        @TypeConverter
        @JvmStatic
        fun toConnection(connection: Int): ConnectionStatus = ConnectionStatus.entries[connection]

        @TypeConverter
        @JvmStatic
        fun fromConnection(connection: ConnectionStatus): Int = connection.ordinal

        @TypeConverter
        @JvmStatic
        fun toSender(sender: Int): Sender = Sender.entries[sender]

        @TypeConverter
        @JvmStatic
        fun fromSender(sender: Sender): Int = sender.ordinal

        @TypeConverter
        @JvmStatic
        fun toMessageType(type: Int): MessageType = MessageType.entries[type]

        @TypeConverter
        @JvmStatic
        fun fromMessageType(type: MessageType): Int = type.ordinal
    }
}

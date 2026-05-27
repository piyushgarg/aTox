package ltd.evilcorp.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ltd.evilcorp.core.db.entity.ContactEntity
import ltd.evilcorp.core.db.entity.FileTransferEntity
import ltd.evilcorp.core.db.entity.FriendRequestEntity
import ltd.evilcorp.core.db.entity.GroupEntity
import ltd.evilcorp.core.db.entity.GroupMessageEntity
import ltd.evilcorp.core.db.entity.GroupPeerEntity
import ltd.evilcorp.core.db.entity.MessageEntity
import ltd.evilcorp.core.db.entity.UserEntity

@Database(
    entities = [
        ContactEntity::class,
        FileTransferEntity::class,
        FriendRequestEntity::class,
        MessageEntity::class,
        UserEntity::class,
        GroupEntity::class,
        GroupMessageEntity::class,
        GroupPeerEntity::class
    ],
    version = 8,
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun fileTransferDao(): FileTransferDao
    abstract fun friendRequestDao(): FriendRequestDao
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun groupMessageDao(): GroupMessageDao
    abstract fun groupPeerDao(): GroupPeerDao
}

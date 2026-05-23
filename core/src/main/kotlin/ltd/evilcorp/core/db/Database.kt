package ltd.evilcorp.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupMessage
import ltd.evilcorp.core.model.GroupPeer
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.core.model.User

@Database(
    entities = [Contact::class, FileTransfer::class, FriendRequest::class, Message::class, User::class, Group::class, GroupMessage::class, GroupPeer::class],
    version = 7,
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

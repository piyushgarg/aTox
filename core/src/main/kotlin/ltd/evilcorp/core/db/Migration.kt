package ltd.evilcorp.core.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) = db.execSQL(
        "ALTER TABLE contacts ADD COLUMN has_unread_messages INTEGER NOT NULL DEFAULT 0",
    )
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) = db.execSQL(
        "ALTER TABLE messages ADD COLUMN type INTEGER NOT NULL DEFAULT 0",
    )
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS 'file_transfers'")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS 'file_transfers' (
                'id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                'public_key' TEXT NOT NULL,
                'file_number' INTEGER NOT NULL,
                'file_kind' INTEGER NOT NULL,
                'file_size' INTEGER NOT NULL,
                'file_name' TEXT NOT NULL,
                'destination' TEXT NOT NULL,
                'outgoing' INTEGER NOT NULL,
                'progress' INTEGER NOT NULL)
            """.trimIndent(),
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) = db.execSQL(
        "ALTER TABLE contacts ADD COLUMN draft_message TEXT NOT NULL DEFAULT ''",
    )
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) = db.execSQL(
        "ALTER TABLE contacts ADD COLUMN last_online INTEGER NOT NULL DEFAULT 0",
    )
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `groups` (
                `chat_id` TEXT NOT NULL, 
                `name` TEXT NOT NULL, 
                `topic` TEXT NOT NULL, 
                `password_protected` INTEGER NOT NULL, 
                `privacy_state` TEXT NOT NULL, 
                `peer_count` INTEGER NOT NULL, 
                `self_peer_id` INTEGER NOT NULL, 
                `self_role` TEXT NOT NULL, 
                `last_message` INTEGER NOT NULL, 
                `has_unread_messages` INTEGER NOT NULL, 
                `draft_message` TEXT NOT NULL, 
                `connected` INTEGER NOT NULL, 
                `group_number` INTEGER NOT NULL, 
                PRIMARY KEY(`chat_id`))
            """.trimIndent()
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `group_messages` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `group_chat_id` TEXT NOT NULL, 
                `peer_id` INTEGER NOT NULL, 
                `sender_name` TEXT NOT NULL, 
                `message` TEXT NOT NULL, 
                `sender` INTEGER NOT NULL, 
                `type` INTEGER NOT NULL, 
                `correlation_id` INTEGER NOT NULL, 
                `timestamp` INTEGER NOT NULL)
            """.trimIndent()
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `group_peers` (
                `group_chat_id` TEXT NOT NULL, 
                `peer_id` INTEGER NOT NULL, 
                `name` TEXT NOT NULL, 
                `public_key` TEXT NOT NULL, 
                `role` TEXT NOT NULL, 
                `is_ourselves` INTEGER NOT NULL, 
                `status` INTEGER NOT NULL, 
                PRIMARY KEY(`group_chat_id`, `peer_id`))
            """.trimIndent()
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) = db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_messages_conversation_id ON messages(conversation, id)",
    )
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)

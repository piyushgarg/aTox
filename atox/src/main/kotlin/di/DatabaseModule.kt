// SPDX-FileCopyrightText: 2019-2024 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import ltd.evilcorp.core.db.ALL_MIGRATIONS
import ltd.evilcorp.core.db.ContactDao
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.core.db.FileTransferDao
import ltd.evilcorp.core.db.FriendRequestDao
import ltd.evilcorp.core.db.GroupDao
import ltd.evilcorp.core.db.GroupMessageDao
import ltd.evilcorp.core.db.GroupPeerDao
import ltd.evilcorp.core.db.MessageDao
import ltd.evilcorp.core.db.UserDao

import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(appContext: Context): Database =
        Room.databaseBuilder(appContext, Database::class.java, "core_db")
            .addMigrations(*ALL_MIGRATIONS)
            .build()
}

@Module
@InstallIn(SingletonComponent::class)
class DaoModule {
    @Singleton
    @Provides
    internal fun provideContactDao(db: Database): ContactDao = db.contactDao()

    @Singleton
    @Provides
    internal fun provideFileTransferDao(db: Database): FileTransferDao = db.fileTransferDao()

    @Singleton
    @Provides
    internal fun provideFriendRequestDao(db: Database): FriendRequestDao = db.friendRequestDao()

    @Singleton
    @Provides
    internal fun provideMessageDao(db: Database): MessageDao = db.messageDao()

    @Singleton
    @Provides
    internal fun provideUserDao(db: Database): UserDao = db.userDao()

    @Singleton
    @Provides
    internal fun provideGroupDao(db: Database): GroupDao = db.groupDao()

    @Singleton
    @Provides
    internal fun provideGroupMessageDao(db: Database): GroupMessageDao = db.groupMessageDao()

    @Singleton
    @Provides
    internal fun provideGroupPeerDao(db: Database): GroupPeerDao = db.groupPeerDao()
}

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @dagger.Binds
    fun bindContactRepository(impl: ltd.evilcorp.core.repository.ContactRepository): ltd.evilcorp.domain.repository.IContactRepository

    @dagger.Binds
    fun bindUserRepository(impl: ltd.evilcorp.core.repository.UserRepository): ltd.evilcorp.domain.repository.IUserRepository

    @dagger.Binds
    fun bindMessageRepository(impl: ltd.evilcorp.core.repository.MessageRepository): ltd.evilcorp.domain.repository.IMessageRepository

    @dagger.Binds
    fun bindFileTransferRepository(impl: ltd.evilcorp.core.repository.FileTransferRepository): ltd.evilcorp.domain.repository.IFileTransferRepository

    @dagger.Binds
    fun bindUserSettingsRepository(impl: ltd.evilcorp.core.repository.UserSettingsRepository): ltd.evilcorp.domain.repository.IUserSettingsRepository

    @dagger.Binds
    fun bindFriendRequestRepository(impl: ltd.evilcorp.core.repository.FriendRequestRepository): ltd.evilcorp.domain.repository.IFriendRequestRepository

    @dagger.Binds
    fun bindGroupRepository(impl: ltd.evilcorp.core.repository.GroupRepository): ltd.evilcorp.domain.repository.IGroupRepository

    @dagger.Binds
    fun bindAvatarStorage(impl: ltd.evilcorp.core.repository.AvatarStorageImpl): ltd.evilcorp.domain.repository.IAvatarStorage
}


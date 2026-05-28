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
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.core.db.dao.ContactDao
import ltd.evilcorp.core.db.dao.FileTransferDao
import ltd.evilcorp.core.db.dao.FriendRequestDao
import ltd.evilcorp.core.db.dao.GroupDao
import ltd.evilcorp.core.db.dao.GroupMessageDao
import ltd.evilcorp.core.db.dao.GroupPeerDao
import ltd.evilcorp.core.db.dao.MessageDao
import ltd.evilcorp.core.db.dao.UserDao
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import ltd.evilcorp.core.repository.ContactRepositoryImpl
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.core.repository.UserRepositoryImpl
import ltd.evilcorp.domain.features.auth.repository.IUserRepository
import ltd.evilcorp.core.repository.MessageRepositoryImpl
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.core.repository.FileTransferRepositoryImpl
import ltd.evilcorp.domain.features.transfer.repository.IFileTransferRepository
import ltd.evilcorp.core.repository.UserSettingsRepositoryImpl
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository
import ltd.evilcorp.core.repository.FriendRequestRepositoryImpl
import ltd.evilcorp.domain.features.contacts.repository.IFriendRequestRepository
import ltd.evilcorp.core.repository.GroupRepositoryImpl
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import ltd.evilcorp.core.repository.AvatarRepositoryImpl
import ltd.evilcorp.domain.features.auth.repository.IAvatarRepository

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
    fun bindContactRepository(impl: ContactRepositoryImpl): IContactRepository

    @dagger.Binds
    fun bindUserRepository(impl: UserRepositoryImpl): IUserRepository

    @dagger.Binds
    fun bindMessageRepository(impl: MessageRepositoryImpl): IMessageRepository

    @dagger.Binds
    fun bindFileTransferRepository(impl: FileTransferRepositoryImpl): IFileTransferRepository

    @dagger.Binds
    fun bindUserSettingsRepository(impl: UserSettingsRepositoryImpl): IUserSettingsRepository

    @dagger.Binds
    fun bindFriendRequestRepository(impl: FriendRequestRepositoryImpl): IFriendRequestRepository

    @dagger.Binds
    fun bindGroupRepository(impl: GroupRepositoryImpl): IGroupRepository

    @dagger.Binds
    fun bindAvatarRepository(impl: AvatarRepositoryImpl): IAvatarRepository
}

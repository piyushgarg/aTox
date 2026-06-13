// SPDX-FileCopyrightText: 2019-2024 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.di

import dagger.Module
import dagger.Provides
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

import ltd.evilcorp.core.db.ProfileDatabaseProvider
import ltd.evilcorp.core.db.DatabaseCloseOnSwitch

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @DatabaseCloseOnSwitch
    @Suppress("FunctionOnlyReturningConstant")
    fun provideCloseOnSwitch(): Boolean = true

    @Provides
    fun provideDatabase(provider: ProfileDatabaseProvider): Database {
        return provider.getDatabase()
    }
}

@Module
@InstallIn(SingletonComponent::class)
class DaoModule {
    @Provides
    internal fun provideContactDao(db: Database): ContactDao = db.contactDao()

    @Provides
    internal fun provideFileTransferDao(db: Database): FileTransferDao = db.fileTransferDao()

    @Provides
    internal fun provideFriendRequestDao(db: Database): FriendRequestDao = db.friendRequestDao()

    @Provides
    internal fun provideMessageDao(db: Database): MessageDao = db.messageDao()

    @Provides
    internal fun provideUserDao(db: Database): UserDao = db.userDao()

    @Provides
    internal fun provideGroupDao(db: Database): GroupDao = db.groupDao()

    @Provides
    internal fun provideGroupMessageDao(db: Database): GroupMessageDao = db.groupMessageDao()

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

// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import ltd.evilcorp.domain.core.network.bootstrap.IBootstrapNodeRegistry
import ltd.evilcorp.domain.core.network.bootstrap.DefaultBootstrapNodeRegistry
import ltd.evilcorp.domain.core.network.bootstrap.IBootstrapNodeJsonSource
import ltd.evilcorp.atox.infrastructure.tox.AndroidBootstrapNodeJsonSource
import ltd.evilcorp.domain.features.settings.ISettingsFileProcessor
import ltd.evilcorp.atox.ui.settings.AndroidSettingsFileProcessor
import ltd.evilcorp.domain.features.settings.IRunAtStartupController
import ltd.evilcorp.atox.infrastructure.settings.AndroidRunAtStartupController
import ltd.evilcorp.domain.features.call.IProximityManager
import ltd.evilcorp.atox.infrastructure.service.AndroidProximityManager
import ltd.evilcorp.domain.core.network.INotificationManager
import ltd.evilcorp.atox.ui.AndroidNotificationManager
import ltd.evilcorp.domain.core.network.INotificationHelper
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.domain.core.network.IToxStarter
import ltd.evilcorp.atox.infrastructure.tox.ToxStarter
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.IToxProfile
import ltd.evilcorp.domain.core.network.IToxMessenger
import ltd.evilcorp.domain.core.network.IToxFileTransmitter
import ltd.evilcorp.domain.core.network.IToxCallController
import ltd.evilcorp.domain.core.network.IToxGroupManager
import ltd.evilcorp.core.tox.ToxImpl
import ltd.evilcorp.domain.features.call.IAudioRoutingManager
import ltd.evilcorp.atox.infrastructure.media.AudioRoutingManager
import ltd.evilcorp.domain.features.transfer.IFileTransferPlatformHelper
import ltd.evilcorp.core.platform.storage.FileTransferPlatformHelperImpl
import ltd.evilcorp.domain.core.network.save.IToxSaveTester
import ltd.evilcorp.core.tox.save.ToxSaveTesterImpl
import ltd.evilcorp.domain.features.auth.repository.IProfileRepository
import ltd.evilcorp.core.repository.ProfileRepositoryImpl
import ltd.evilcorp.domain.features.backup.repository.IChatHistoryBackupHelper
import ltd.evilcorp.core.platform.backup.ChatHistoryBackupHelperImpl
import ltd.evilcorp.domain.features.backup.repository.IContactsBackupHelper
import ltd.evilcorp.core.platform.backup.ContactsBackupHelperImpl
import ltd.evilcorp.domain.features.backup.repository.IFileTransferBackupHelper
import ltd.evilcorp.core.platform.backup.FileTransferBackupHelperImpl
import ltd.evilcorp.domain.core.network.ITimeProvider
import ltd.evilcorp.core.platform.system.SystemTimeProviderImpl
import ltd.evilcorp.domain.features.call.service.IAudioRecorder
import ltd.evilcorp.core.platform.media.recording.CallAudioRecorderImpl
import ltd.evilcorp.domain.features.call.service.ICallSignalPlayer
import ltd.evilcorp.core.platform.media.playback.CallSignalPlayerImpl
import ltd.evilcorp.domain.features.group.IGroupConnectionScheduler
import ltd.evilcorp.core.tox.GroupConnectionSchedulerImpl
import ltd.evilcorp.domain.features.transfer.IFileStorageHelper
import ltd.evilcorp.core.platform.storage.JVMFileStorageHelperImpl
import ltd.evilcorp.domain.features.call.service.IVoiceRecorder
import ltd.evilcorp.core.platform.media.recording.VoiceRecorderImpl
import ltd.evilcorp.atox.ui.chat.FileExporter
import ltd.evilcorp.atox.ui.chat.AndroidFileExporter
import ltd.evilcorp.atox.ui.createprofile.ProfileBackupProcessor
import ltd.evilcorp.atox.ui.createprofile.AndroidProfileBackupProcessor
import ltd.evilcorp.domain.core.network.IFileStorageProvider
import ltd.evilcorp.core.platform.storage.AndroidFileStorageProvider
import ltd.evilcorp.domain.features.transfer.IFileTransferSessionRegistry
import ltd.evilcorp.core.platform.storage.FileTransferSessionRegistryImpl
import ltd.evilcorp.domain.features.group.IGroupSessionRegistry
import ltd.evilcorp.core.platform.storage.GroupSessionRegistryImpl
import ltd.evilcorp.domain.features.call.ICallSessionRegistry
import ltd.evilcorp.core.platform.storage.CallSessionRegistryImpl
import ltd.evilcorp.domain.core.platform.IPlatformServices
import ltd.evilcorp.core.platform.JvmPlatformServices

@Module
@InstallIn(SingletonComponent::class)
@Suppress("ComplexInterface")
interface AppBindingsModule {

    @Binds
    @Singleton
    fun bindBootstrapNodeRegistry(impl: DefaultBootstrapNodeRegistry): IBootstrapNodeRegistry

    @Binds
    @Singleton
    fun bindBootstrapNodeJsonSource(impl: AndroidBootstrapNodeJsonSource): IBootstrapNodeJsonSource

    @Binds
    @Singleton
    fun bindSettingsFileProcessor(impl: AndroidSettingsFileProcessor): ISettingsFileProcessor

    @Binds
    @Singleton
    fun bindProximityManager(impl: AndroidProximityManager): IProximityManager

    @Binds
    @Singleton
    fun bindNotificationManager(impl: AndroidNotificationManager): INotificationManager

    @Binds
    @Singleton
    fun bindNotificationHelper(impl: NotificationHelper): INotificationHelper

    @Binds
    @Singleton
    fun bindToxStarter(impl: ToxStarter): IToxStarter

    @Binds
    @Singleton
    fun bindTox(impl: ToxImpl): ITox

    @Binds
    @Singleton
    fun bindToxProfile(impl: ToxImpl): IToxProfile

    @Binds
    @Singleton
    fun bindToxMessenger(impl: ToxImpl): IToxMessenger

    @Binds
    @Singleton
    fun bindToxFileTransmitter(impl: ToxImpl): IToxFileTransmitter

    @Binds
    @Singleton
    fun bindToxCallController(impl: ToxImpl): IToxCallController

    @Binds
    @Singleton
    fun bindToxGroupManager(impl: ToxImpl): IToxGroupManager

    @Binds
    @Singleton
    fun bindAudioRoutingManager(impl: AudioRoutingManager): IAudioRoutingManager

    @Binds
    @Singleton
    fun bindFileTransferPlatformHelper(impl: FileTransferPlatformHelperImpl): IFileTransferPlatformHelper

    @Binds
    @Singleton
    fun bindToxSaveTester(impl: ToxSaveTesterImpl): IToxSaveTester

    @Binds
    @Singleton
    fun bindProfileRepository(impl: ProfileRepositoryImpl): IProfileRepository

    @Binds
    @Singleton
    fun bindChatHistoryBackupHelper(impl: ChatHistoryBackupHelperImpl): IChatHistoryBackupHelper

    @Binds
    @Singleton
    fun bindContactsBackupHelper(impl: ContactsBackupHelperImpl): IContactsBackupHelper

    @Binds
    @Singleton
    fun bindFileTransferBackupHelper(impl: FileTransferBackupHelperImpl): IFileTransferBackupHelper

    @Binds
    @Singleton
    fun bindTimeProvider(impl: SystemTimeProviderImpl): ITimeProvider

    @Binds
    @Singleton
    fun bindAudioRecorder(impl: CallAudioRecorderImpl): IAudioRecorder

    @Binds
    @Singleton
    fun bindCallSignalPlayer(impl: CallSignalPlayerImpl): ICallSignalPlayer

    @Binds
    @Singleton
    fun bindGroupConnectionScheduler(impl: GroupConnectionSchedulerImpl): IGroupConnectionScheduler

    @Binds
    @Singleton
    fun bindFileStorageHelper(impl: JVMFileStorageHelperImpl): IFileStorageHelper

    @Binds
    @Singleton
    fun bindVoiceRecorder(impl: VoiceRecorderImpl): IVoiceRecorder

    @Binds
    @Singleton
    fun bindFileExporter(impl: AndroidFileExporter): FileExporter

    @Binds
    @Singleton
    fun bindProfileBackupProcessor(impl: AndroidProfileBackupProcessor): ProfileBackupProcessor

    @Binds
    @Singleton
    fun bindFileStorageProvider(impl: AndroidFileStorageProvider): IFileStorageProvider

    @Binds
    @Singleton
    fun bindFileTransferSessionRegistry(impl: FileTransferSessionRegistryImpl): IFileTransferSessionRegistry

    @Binds
    @Singleton
    fun bindGroupSessionRegistry(impl: GroupSessionRegistryImpl): IGroupSessionRegistry

    @Binds
    @Singleton
    fun bindCallSessionRegistry(impl: CallSessionRegistryImpl): ICallSessionRegistry

    @Binds
    @Singleton
    fun bindRunAtStartupController(impl: AndroidRunAtStartupController): IRunAtStartupController

    @Binds
    @Singleton
    fun bindPlatformServices(impl: JvmPlatformServices): IPlatformServices

    @Binds
    @Singleton
    fun bindToxFriendEventBus(impl: ltd.evilcorp.atox.infrastructure.tox.ToxFriendEventBusImpl): ltd.evilcorp.domain.features.contacts.IToxFriendEventBus
}

package ltd.evilcorp.atox.di

import android.content.Context
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ltd.evilcorp.atox.infrastructure.tox.AndroidBootstrapNodeJsonSource
import ltd.evilcorp.atox.infrastructure.backup.CallLogBackupDataProvider
import ltd.evilcorp.atox.infrastructure.backup.ChatHistoryBackupDataProvider
import ltd.evilcorp.atox.infrastructure.backup.ContactsBackupDataProvider
import ltd.evilcorp.atox.infrastructure.backup.FileTransferHistoryBackupDataProvider
import ltd.evilcorp.atox.infrastructure.backup.ToxCoreBackupDataProvider
import ltd.evilcorp.atox.infrastructure.backup.TransferredFilesBackupDataProvider
import ltd.evilcorp.domain.tox.bootstrap.BootstrapNodeRegistry
import ltd.evilcorp.domain.tox.bootstrap.BootstrapNodeJsonSource
import ltd.evilcorp.domain.tox.bootstrap.DefaultBootstrapNodeRegistry
import ltd.evilcorp.core.tox.save.AndroidSaveManager
import ltd.evilcorp.core.tox.save.SaveManager
import ltd.evilcorp.domain.backup.BackupDataProvider

import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    fun provideBootstrapNodeRegistry(nodeRegistry: DefaultBootstrapNodeRegistry): BootstrapNodeRegistry = nodeRegistry

    @Provides
    fun provideBootstrapNodeJsonSource(source: AndroidBootstrapNodeJsonSource): BootstrapNodeJsonSource = source

    @Provides
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.Default)

    @Provides
    fun provideSaveManager(ctx: Context): SaveManager = AndroidSaveManager(ctx)

    @Provides
    fun provideBackupDataProviders(
        toxCore: ToxCoreBackupDataProvider,
        contacts: ContactsBackupDataProvider,
        chatHistory: ChatHistoryBackupDataProvider,
        callLog: CallLogBackupDataProvider,
        fileTransferHistory: FileTransferHistoryBackupDataProvider,
        transferredFiles: TransferredFilesBackupDataProvider,
    ): List<BackupDataProvider> = listOf(
        toxCore,
        contacts,
        chatHistory,
        callLog,
        fileTransferHistory,
        transferredFiles,
    )

    @Provides
    fun provideFileExporter(exporter: ltd.evilcorp.atox.ui.chat.AndroidFileExporter): ltd.evilcorp.atox.ui.chat.FileExporter = exporter

    @Provides
    fun provideSettingsFileProcessor(processor: ltd.evilcorp.atox.ui.settings.AndroidSettingsFileProcessor): ltd.evilcorp.domain.feature.ISettingsFileProcessor = processor

    @Provides
    fun provideProximityManager(manager: ltd.evilcorp.atox.infrastructure.service.AndroidProximityManager): ltd.evilcorp.domain.feature.ProximityManager = manager

    @Provides
    fun provideNotificationManager(manager: ltd.evilcorp.atox.ui.AndroidNotificationManager): ltd.evilcorp.domain.feature.NotificationManager = manager

    @Provides
    fun provideProfileBackupProcessor(processor: ltd.evilcorp.atox.ui.createprofile.AndroidProfileBackupProcessor): ltd.evilcorp.atox.ui.createprofile.ProfileBackupProcessor = processor

    @Provides
    fun provideNotificationHelper(helper: ltd.evilcorp.atox.ui.NotificationHelper): ltd.evilcorp.domain.feature.INotificationHelper = helper

    @Provides
    fun provideToxStarter(starter: ltd.evilcorp.atox.infrastructure.tox.ToxStarter): ltd.evilcorp.domain.tox.IToxStarter = starter

    @Provides
    fun provideTox(impl: ltd.evilcorp.core.tox.Tox): ltd.evilcorp.domain.tox.ITox = impl

    @Provides
    fun provideAudioRoutingManager(manager: ltd.evilcorp.atox.infrastructure.media.AudioRoutingManager): ltd.evilcorp.domain.feature.IAudioRoutingManager = manager

    @Provides
    fun provideFileTransferPlatformHelper(impl: ltd.evilcorp.core.repository.FileTransferPlatformHelperImpl): ltd.evilcorp.domain.feature.IFileTransferPlatformHelper = impl

    @Provides
    fun provideToxSaveTester(tester: ltd.evilcorp.core.tox.save.ToxSaveTesterImpl): ltd.evilcorp.domain.tox.save.IToxSaveTester = tester

    @Provides
    fun provideProfileDeleter(deleter: ltd.evilcorp.core.repository.ProfileDeleterImpl): ltd.evilcorp.domain.usecase.IProfileDeleter = deleter
}

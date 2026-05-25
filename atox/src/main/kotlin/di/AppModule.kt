package ltd.evilcorp.atox.di

import android.content.Context
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ltd.evilcorp.atox.tox.AndroidBootstrapNodeJsonSource
import ltd.evilcorp.atox.backup.CallLogBackupDataProvider
import ltd.evilcorp.atox.backup.ChatHistoryBackupDataProvider
import ltd.evilcorp.atox.backup.ContactsBackupDataProvider
import ltd.evilcorp.atox.backup.FileTransferHistoryBackupDataProvider
import ltd.evilcorp.atox.backup.ToxCoreBackupDataProvider
import ltd.evilcorp.atox.backup.TransferredFilesBackupDataProvider
import ltd.evilcorp.core.tox.bootstrap.BootstrapNodeRegistry
import ltd.evilcorp.core.tox.bootstrap.BootstrapNodeJsonSource
import ltd.evilcorp.core.tox.bootstrap.DefaultBootstrapNodeRegistry
import ltd.evilcorp.core.tox.save.AndroidSaveManager
import ltd.evilcorp.core.tox.save.SaveManager
import ltd.evilcorp.domain.backup.BackupDataProvider

@Module
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
    fun provideProximityManager(manager: ltd.evilcorp.atox.service.AndroidProximityManager): ltd.evilcorp.domain.feature.ProximityManager = manager

    @Provides
    fun provideNotificationManager(manager: ltd.evilcorp.atox.ui.AndroidNotificationManager): ltd.evilcorp.domain.feature.NotificationManager = manager

    @Provides
    fun provideProfileBackupProcessor(processor: ltd.evilcorp.atox.ui.createprofile.AndroidProfileBackupProcessor): ltd.evilcorp.atox.ui.createprofile.ProfileBackupProcessor = processor

    @Provides
    fun provideNotificationHelper(helper: ltd.evilcorp.atox.ui.NotificationHelper): ltd.evilcorp.domain.feature.INotificationHelper = helper

    @Provides
    fun provideToxStarter(starter: ltd.evilcorp.atox.tox.ToxStarter): ltd.evilcorp.domain.tox.IToxStarter = starter
}

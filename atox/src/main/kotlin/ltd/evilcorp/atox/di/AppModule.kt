// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ltd.evilcorp.atox.infrastructure.backup.CallLogBackupDataProvider
import ltd.evilcorp.atox.infrastructure.backup.ChatHistoryBackupDataProvider
import ltd.evilcorp.atox.infrastructure.backup.ContactsBackupDataProvider
import ltd.evilcorp.atox.infrastructure.backup.FileTransferHistoryBackupDataProvider
import ltd.evilcorp.atox.infrastructure.backup.ToxCoreBackupDataProvider
import ltd.evilcorp.atox.infrastructure.backup.TransferredFilesBackupDataProvider
import ltd.evilcorp.core.tox.save.AndroidSaveManagerImpl
import ltd.evilcorp.domain.core.network.save.ISaveManager
import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun provideSaveManager(ctx: Context): ISaveManager = AndroidSaveManagerImpl(ctx)

    @Provides
    fun provideBackupDataProviders(
        toxCore: ToxCoreBackupDataProvider,
        contacts: ContactsBackupDataProvider,
        chatHistory: ChatHistoryBackupDataProvider,
        callLog: CallLogBackupDataProvider,
        fileTransferHistory: FileTransferHistoryBackupDataProvider,
        transferredFiles: TransferredFilesBackupDataProvider,
    ): List<IBackupDataProvider> = listOf(
        toxCore,
        contacts,
        chatHistory,
        callLog,
        fileTransferHistory,
        transferredFiles,
    )
}

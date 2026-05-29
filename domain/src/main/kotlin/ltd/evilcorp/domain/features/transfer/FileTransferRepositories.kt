// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.transfer

import javax.inject.Inject
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.features.transfer.repository.IFileTransferRepository
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository

/**
 * Aggregates all repository interfaces consumed by FileTransferManager,
 * simplifying DI binding and constructor dependency injection constraints.
 */
class FileTransferRepositories @Inject constructor(
    val contact: IContactRepository,
    val message: IMessageRepository,
    val transfer: IFileTransferRepository,
    val userSettings: IUserSettingsRepository,
)

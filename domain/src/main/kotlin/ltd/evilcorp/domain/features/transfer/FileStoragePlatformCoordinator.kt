// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.transfer

import javax.inject.Inject

/**
 * Encapsulates Android-specific and cross-platform file saving and
 * storage interaction components for clean modular separation.
 */
class FileStoragePlatformCoordinator @Inject constructor(
    val platformHelper: IFileTransferPlatformHelper,
    val fileStorageHelper: IFileStorageHelper,
)

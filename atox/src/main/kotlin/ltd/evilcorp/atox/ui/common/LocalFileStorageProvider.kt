// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import ltd.evilcorp.domain.core.network.IFileStorageProvider

val LocalFileStorageProvider = staticCompositionLocalOf<IFileStorageProvider> {
    error("No IFileStorageProvider provided")
}

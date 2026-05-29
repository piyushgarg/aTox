// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import javax.inject.Inject

/**
 * Aggregates Use Cases related to file transfers in group chats
 * to reduce ViewModel constructor dependencies.
 */
class GroupFileTransferActions @Inject constructor(
    val acceptGroupFileTransfer: AcceptGroupFileTransferUseCase,
    val rejectGroupFileTransfer: RejectGroupFileTransferUseCase,
    val cancelGroupFileTransfer: CancelGroupFileTransferUseCase,
    val saveGroupFileTransfer: SaveGroupFileTransferUseCase,
)

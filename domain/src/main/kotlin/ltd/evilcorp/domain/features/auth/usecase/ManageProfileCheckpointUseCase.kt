// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.auth.usecase

import ltd.evilcorp.domain.features.auth.repository.IProfileRepository
import javax.inject.Inject

sealed interface CheckpointAction {
    object Create : CheckpointAction
    object Clear : CheckpointAction
    object Restore : CheckpointAction
}

/**
 * Use case to manage transactional SQLite checkpoint backups (creating, clearing, or restoring on backup failure).
 */
class ManageProfileCheckpointUseCase @Inject constructor(
    private val profileRepository: IProfileRepository,
) {
    suspend fun execute(action: CheckpointAction): Boolean {
        return when (action) {
            CheckpointAction.Create -> profileRepository.createCheckpoint()
            CheckpointAction.Clear -> {
                profileRepository.clearCheckpoint()
                true
            }
            CheckpointAction.Restore -> {
                profileRepository.restoreFromCheckpoint()
                true
            }
        }
    }
}

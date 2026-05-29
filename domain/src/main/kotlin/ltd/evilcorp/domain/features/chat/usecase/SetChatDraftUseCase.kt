// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.chat.usecase

import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.core.model.PublicKey
import javax.inject.Inject

/**
 * Use case to set the active composed message draft for a specific contact chat.
 */
class SetChatDraftUseCase @Inject constructor(
    private val contactRepository: IContactRepository,
) {
    suspend fun execute(publicKey: PublicKey, draft: String) {
        contactRepository.setDraftMessage(publicKey.string(), draft)
    }
}

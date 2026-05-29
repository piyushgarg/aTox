// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.contacts.usecase

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.core.model.PublicKey
import javax.inject.Inject

/**
 * Use case to retrieve a single active user contact flow.
 */
class GetContactUseCase @Inject constructor(
    private val contactRepository: IContactRepository,
) {
    fun execute(publicKey: PublicKey): Flow<Contact?> {
        return contactRepository.get(publicKey.string())
    }
}

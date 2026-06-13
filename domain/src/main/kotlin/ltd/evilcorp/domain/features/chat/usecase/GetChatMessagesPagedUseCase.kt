// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.chat.usecase

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import javax.inject.Inject

/**
 * Use case to retrieve the reactive message history stream for a specific contact chat using Paging 3.
 */
class GetChatMessagesPagedUseCase @Inject constructor(
    private val messageRepository: IMessageRepository,
) {
    fun execute(publicKey: PublicKey): Flow<PagingData<Message>> {
        return messageRepository.getPagingFlow(publicKey.string())
    }
}


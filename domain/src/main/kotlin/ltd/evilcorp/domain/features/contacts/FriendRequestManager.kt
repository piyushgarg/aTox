// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.contacts

import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.contacts.repository.IFriendRequestRepository
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.core.network.IToxProfile

class FriendRequestManager @Inject constructor(
    private val scope: CoroutineScope,
    private val contactRepository: IContactRepository,
    private val friendRequestRepository: IFriendRequestRepository,
    private val messageRepository: IMessageRepository,
    private val tox: IToxProfile,
) {
    fun getAll(): Flow<List<FriendRequest>> = friendRequestRepository.getAll()
    fun get(id: PublicKey): Flow<FriendRequest?> = friendRequestRepository.get(id.string())

    fun accept(friendRequest: FriendRequest) = scope.launch {
        val acceptTime = System.currentTimeMillis()
        tox.acceptFriendRequest(PublicKey(friendRequest.publicKey))
        messageRepository.add(
            Message(
                friendRequest.publicKey,
                friendRequest.message,
                Sender.Received,
                MessageType.Normal,
                0,
                acceptTime,
            ),
        )
        contactRepository.add(Contact(friendRequest.publicKey))
        contactRepository.setLastMessage(friendRequest.publicKey, acceptTime)
        friendRequestRepository.delete(friendRequest)
    }

    fun reject(friendRequest: FriendRequest) = scope.launch { friendRequestRepository.delete(friendRequest) }
}

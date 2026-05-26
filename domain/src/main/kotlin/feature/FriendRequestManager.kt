// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.feature

import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.repository.IContactRepository
import ltd.evilcorp.domain.repository.IFriendRequestRepository
import ltd.evilcorp.domain.repository.IMessageRepository
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.domain.tox.ITox

class FriendRequestManager @Inject constructor(
    private val scope: CoroutineScope,
    private val contactRepository: IContactRepository,
    private val friendRequestRepository: IFriendRequestRepository,
    private val messageRepository: IMessageRepository,
    private val tox: ITox,
) {
    fun getAll(): Flow<List<FriendRequest>> = friendRequestRepository.getAll()
    fun get(id: PublicKey): Flow<FriendRequest?> = friendRequestRepository.get(id.string())

    fun accept(friendRequest: FriendRequest) = scope.launch {
        val acceptTime = Date().time
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

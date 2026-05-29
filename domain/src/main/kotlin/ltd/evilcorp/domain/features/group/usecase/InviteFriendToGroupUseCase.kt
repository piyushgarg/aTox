package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.group.GroupMessagingService
import javax.inject.Inject

/**
 * Use case to invite a friend (by their contact public key) to a group chat.
 */
class InviteFriendToGroupUseCase @Inject constructor(
    private val groupMessagingService: GroupMessagingService,
) {
    suspend fun execute(chatId: String, friendPublicKey: String) {
        groupMessagingService.inviteFriend(chatId, friendPublicKey)
    }
}

// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import javax.inject.Inject

/**
 * Aggregates Use Cases related to messaging in group chats
 * to reduce ViewModel constructor dependencies.
 */
class GroupChatActions @Inject constructor(
    val sendGroupMessage: SendGroupMessageUseCase,
    val sendGroupFile: SendGroupFileUseCase,
    val sendGroupVoice: SendGroupVoiceUseCase,
    val clearGroupHistory: ClearGroupHistoryUseCase,
    val deleteGroupMessage: DeleteGroupMessageUseCase,
    val setGroupDraft: SetGroupDraftUseCase,
)

// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group

import javax.inject.Inject
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository

/**
 * Encapsulates repository layers for group chat databases, ensuring clean
 * boundary separation and reducing constructor parameter counts.
 */
class GroupDataRepositories @Inject constructor(
    val group: IGroupRepository,
    val contact: IContactRepository,
    val message: IMessageRepository,
)

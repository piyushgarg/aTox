// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group

import javax.inject.Inject

/**
 * Aggregates group connection and group messaging services
 * to keep Downstream manager parameters within architectural boundaries.
 */
class GroupServices @Inject constructor(
    val connection: GroupConnectionService,
    val messaging: GroupMessagingService,
)

// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group

import javax.inject.Inject

/**
 * Aggregates group connection scheduling and active session registrations
 * to keep Downstream manager parameters within architectural boundaries.
 */
class GroupSessionCoordinator @Inject constructor(
    val connectionScheduler: IGroupConnectionScheduler,
    val sessionRegistry: IGroupSessionRegistry,
)

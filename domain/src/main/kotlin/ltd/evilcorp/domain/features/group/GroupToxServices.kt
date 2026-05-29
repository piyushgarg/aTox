// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group

import javax.inject.Inject
import ltd.evilcorp.domain.core.network.IToxGroupManager
import ltd.evilcorp.domain.core.network.IToxProfile

/**
 * Encapsulates group JNI Tox interaction interfaces, ensuring clean
 * boundary separation and reducing constructor parameter counts.
 */
class GroupToxServices @Inject constructor(
    val tox: IToxGroupManager,
    val profile: IToxProfile,
)

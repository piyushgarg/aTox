// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.auth.usecase

import ltd.evilcorp.domain.core.network.IToxProfile
import javax.inject.Inject

class GetSelfNameUseCase @Inject constructor(
    private val tox: IToxProfile
) {
    fun execute(): String {
        return tox.getName()
    }
}

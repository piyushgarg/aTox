// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.auth.model

data class ProfileInfo(
    val id: String,
    val name: String,
    val avatarUri: String? = null
)

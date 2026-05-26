// SPDX-FileCopyrightText: 2019-2020 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.model

enum class ProxyStatus {
    Good,
    BadPort,
    BadHost,
    BadType,
    NotFound,
}

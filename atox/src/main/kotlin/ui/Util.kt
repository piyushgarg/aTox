// SPDX-FileCopyrightText: 2019-2022 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2021-2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui

import android.content.res.Resources
import android.util.TypedValue
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.Contact



internal sealed interface Size

@JvmInline
internal value class Px(val px: Int) : Size

@JvmInline
internal value class Dp(val dp: Float) : Size {
    fun asPx(res: Resources): Px =
        Px(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.displayMetrics).toInt())
}

private const val OFFLINE_CONTACT_SORT_PRIORITY = -1000L

internal fun contactListSorter(contact: Contact) = when {
    contact.lastMessage != 0L -> contact.lastMessage
    contact.connectionStatus == ConnectionStatus.None -> OFFLINE_CONTACT_SORT_PRIORITY
    else -> -contact.status.ordinal.toLong()
}

fun stripReplyPrefix(text: String): String {
    if (text.startsWith("[reply:")) {
        val index = text.indexOf("] ")
        if (index != -1) {
            return text.substring(index + 2)
        }
    }
    return text
}

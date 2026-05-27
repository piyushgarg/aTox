// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.model

data class ReplyInfo(
    val isReply: Boolean,
    val parentIdentifier: String,
    val actualText: String
)

object ReplyParser {
    fun parse(messageText: String): ReplyInfo {
        if (messageText.startsWith("[reply:")) {
            val index = messageText.indexOf("] ")
            if (index != -1) {
                val tag = messageText.substring(0, index + 1)
                val parentIdentifier = tag.removePrefix("[reply:").removeSuffix("]")
                val actualText = messageText.substring(index + 2)
                return ReplyInfo(
                    isReply = true,
                    parentIdentifier = parentIdentifier,
                    actualText = actualText
                )
            }
        }
        return ReplyInfo(
            isReply = false,
            parentIdentifier = "",
            actualText = messageText
        )
    }

    fun stripReplyPrefix(text: String): String {
        return parse(text).actualText
    }
}

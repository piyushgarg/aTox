// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network.enums

/**
 * Enum representing potential bitwise flags for audio and video call states.
 * Combined into an EnumSet on the Kotlin side when receiving a bitmask from the native core.
 */
enum class ToxavFriendCallState {
    /** An error occurred during the call. */
    Error,
    /** The call completed successfully. */
    Finished,
    /** We are sending outgoing audio (voice). */
    SendingAudio,
    /** We are sending outgoing video (camera stream). */
    SendingVideo,
    /** The peer is broadcasting audio to us (we are receiving/playing it). */
    ReceivingAudio,
    /** The peer is broadcasting video to us (we are receiving/rendering it). */
    ReceivingVideo
}

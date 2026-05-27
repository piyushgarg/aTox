// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox.enums

/**
 * Enum representing call control commands for active multimedia calls in ToxAV.
 * Matches `Toxav_Call_Control` codes in toxcore.
 */
enum class ToxavCallControl {
    /** Resume a previously paused call. */
    RESUME,
    /** Pause (put on hold) the call. */
    PAUSE,
    /** Reject an incoming call or terminate an active session. */
    CANCEL,
    /** Mute the microphone (stop sending outgoing audio). */
    MUTE_AUDIO,
    /** Unmute the microphone (resume sending audio). */
    UNMUTE_AUDIO,
    /** Hide the camera (stop sending outgoing video stream). */
    HIDE_VIDEO,
    /** Show the camera (resume sending video stream). */
    SHOW_VIDEO
}

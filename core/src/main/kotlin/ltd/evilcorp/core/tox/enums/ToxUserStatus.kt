package ltd.evilcorp.core.tox.enums

import ltd.evilcorp.core.model.UserStatus

enum class ToxUserStatus {
    NONE,
    AWAY,
    BUSY;

    fun toUserStatus(): UserStatus = when (this) {
        NONE -> UserStatus.None
        AWAY -> UserStatus.Away
        BUSY -> UserStatus.Busy
    }

    companion object {
        fun fromInt(value: Int): ToxUserStatus = values().getOrElse(value) { NONE }
    }
}

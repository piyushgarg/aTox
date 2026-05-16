package ltd.evilcorp.domain.tox.enums

import ltd.evilcorp.core.vo.ConnectionStatus

enum class ToxConnection {
    NONE,
    TCP,
    UDP;

    fun toConnectionStatus(): ConnectionStatus = when (this) {
        NONE -> ConnectionStatus.None
        TCP -> ConnectionStatus.TCP
        UDP -> ConnectionStatus.UDP
    }

    companion object {
        fun fromInt(value: Int): ToxConnection = values().getOrElse(value) { NONE }
    }
}

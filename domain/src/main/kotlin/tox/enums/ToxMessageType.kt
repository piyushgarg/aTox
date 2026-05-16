package ltd.evilcorp.domain.tox.enums

enum class ToxMessageType {
    NORMAL,
    ACTION;
 
    fun toMessageType(): ltd.evilcorp.core.vo.MessageType = when (this) {
        NORMAL -> ltd.evilcorp.core.vo.MessageType.Normal
        ACTION -> ltd.evilcorp.core.vo.MessageType.Action
    }

    companion object {
        fun fromInt(value: Int): ToxMessageType = values().getOrElse(value) { NORMAL }
    }
}

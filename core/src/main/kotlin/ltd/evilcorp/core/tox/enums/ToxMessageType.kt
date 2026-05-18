package ltd.evilcorp.core.tox.enums

enum class ToxMessageType {
    NORMAL,
    ACTION;
 
    fun toMessageType(): ltd.evilcorp.core.model.MessageType = when (this) {
        NORMAL -> ltd.evilcorp.core.model.MessageType.Normal
        ACTION -> ltd.evilcorp.core.model.MessageType.Action
    }

    companion object {
        fun fromInt(value: Int): ToxMessageType = values().getOrElse(value) { NORMAL }
    }
}

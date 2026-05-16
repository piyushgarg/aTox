package ltd.evilcorp.domain.tox.enums

enum class ToxFileControl {
    RESUME,
    PAUSE,
    CANCEL;

    companion object {
        fun fromInt(value: Int): ToxFileControl = values().getOrElse(value) { CANCEL }
    }
}

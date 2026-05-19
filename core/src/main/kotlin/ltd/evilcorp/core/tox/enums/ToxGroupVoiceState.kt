package ltd.evilcorp.core.tox.enums

/**
 * Перечисление режимов голосового вещания (голосовых привилегий) в групповом звонке NGC.
 * Соответствует кодам `Tox_Group_Voice_State` в toxcore.
 */
enum class ToxGroupVoiceState(val value: Int) {
    /** Голосовая трансляция разрешена для всех участников группы. */
    ALL(0),
    /** Говорить могут только модераторы и создатель группы. */
    MODERATOR(1),
    /** Голосовое вещание разрешено только создателю/основателю чата. */
    FOUNDER(2);

    companion object {
        /**
         * Возвращает режим голосового вещания по целочисленному коду JNI.
         * @param value Целочисленный код JNI.
         * @return Соответствующий элемент [ToxGroupVoiceState].
         */
        fun fromInt(value: Int): ToxGroupVoiceState =
            values().firstOrNull { it.value == value } ?: ALL
    }
}

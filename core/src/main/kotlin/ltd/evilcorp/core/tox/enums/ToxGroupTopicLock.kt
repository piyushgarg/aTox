package ltd.evilcorp.core.tox.enums

/**
 * Перечисление режимов блокировки изменения темы в групповом чате NGC.
 * Соответствует кодам `Tox_Group_Topic_Lock` в toxcore.
 */
enum class ToxGroupTopicLock(val value: Int) {
    /** Изменение темы заблокировано (только модераторы и создатель могут менять тему). */
    ENABLED(0),
    /** Изменение темы разрешено для всех участников конференции. */
    DISABLED(1);

    companion object {
        /**
         * Возвращает режим блокировки темы по целочисленному коду JNI.
         * @param value Целочисленный код JNI.
         * @return Соответствующий элемент [ToxGroupTopicLock].
         */
        fun fromInt(value: Int): ToxGroupTopicLock =
            values().firstOrNull { it.value == value } ?: ENABLED
    }
}

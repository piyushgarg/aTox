package ltd.evilcorp.core.tox.enums

/**
 * Перечисление причин отказа при попытке входа в групповой чат NGC.
 * Соответствует кодам `Tox_Group_Join_Fail` в toxcore.
 */
enum class ToxGroupJoinFail(val value: Int) {
    /** Превышен лимит участников конференции. */
    PEER_LIMIT(0),
    /** Указан неверный пароль для входа. */
    INVALID_PASSWORD(1),
    /** Неизвестная ошибка подключения. */
    UNKNOWN(2);

    companion object {
        /**
         * Возвращает причину отказа по целочисленному коду JNI.
         * @param value Целочисленный код JNI.
         * @return Соответствующий элемент [ToxGroupJoinFail].
         */
        fun fromInt(value: Int): ToxGroupJoinFail =
            values().firstOrNull { it.value == value } ?: UNKNOWN
    }
}

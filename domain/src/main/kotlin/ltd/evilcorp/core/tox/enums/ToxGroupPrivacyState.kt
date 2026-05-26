package ltd.evilcorp.core.tox.enums

/**
 * Перечисление режимов приватности группового чата NGC (Next Generation Conferences).
 * Соответствует кодам `Tox_Group_Privacy_State` в toxcore.
 */
enum class ToxGroupPrivacyState(val value: Int) {
    /** Публичный чат (открыт для присоединения без пароля). */
    PUBLIC(0),
    /** Приватный чат (требуется приглашение или пароль). */
    PRIVATE(1);

    companion object {
        /**
         * Возвращает режим приватности по целочисленному коду JNI.
         * @param value Целочисленный код JNI.
         * @return Соответствующий элемент [ToxGroupPrivacyState].
         */
        fun fromInt(value: Int): ToxGroupPrivacyState =
            values().firstOrNull { it.value == value } ?: PUBLIC
    }
}

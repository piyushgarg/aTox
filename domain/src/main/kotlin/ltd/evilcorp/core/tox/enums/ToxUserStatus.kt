package ltd.evilcorp.core.tox.enums

import ltd.evilcorp.core.model.UserStatus

/**
 * Перечисление статусов присутствия пользователя Tox (User Status).
 */
enum class ToxUserStatus {
    /** В сети (Online / Ready). */
    NONE,
    /** Отошел (Away). */
    AWAY,
    /** Не беспокоить (Busy). */
    BUSY;

    /**
     * Конвертирует внутренний статус Tox во внешнюю доменную модель [UserStatus].
     */
    fun toUserStatus(): UserStatus = when (this) {
        NONE -> UserStatus.None
        AWAY -> UserStatus.Away
        BUSY -> UserStatus.Busy
    }

    companion object {
        /**
         * Возвращает статус присутствия по его целочисленному коду JNI.
         * @param value Целочисленный код JNI.
         * @return Соответствующий элемент [ToxUserStatus].
         */
        fun fromInt(value: Int): ToxUserStatus = values().getOrElse(value) { NONE }
    }
}

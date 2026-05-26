package ltd.evilcorp.core.tox.enums

/**
 * Перечисление ролей (привилегий) участников в групповом чате NGC.
 * Соответствует кодам `Tox_Group_Role` в toxcore.
 */
enum class ToxGroupRole(val value: Int) {
    /** Создатель/Основатель группы (полный контроль над правами и паролями). */
    FOUNDER(0),
    /** Модератор чата (может кикать и регулировать голос/статусы участников). */
    MODERATOR(1),
    /** Обычный Пользователь (с возможностью отправки текста/звука). */
    USER(2),
    /** Наблюдатель (не имеет прав отправлять сообщения или говорить). */
    OBSERVER(3);

    companion object {
        /**
         * Возвращает роль участника по ее целочисленному коду JNI.
         * @param value Целочисленный код JNI.
         * @return Соответствующий элемент [ToxGroupRole].
         */
        fun fromInt(value: Int): ToxGroupRole =
            values().firstOrNull { it.value == value } ?: USER
    }
}

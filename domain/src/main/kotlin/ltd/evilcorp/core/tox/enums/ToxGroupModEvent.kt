package ltd.evilcorp.core.tox.enums

/**
 * Перечисление административных действий (модераторских событий) в групповом чате NGC.
 * Соответствует кодам `Tox_Group_Mod_Event` в toxcore.
 */
enum class ToxGroupModEvent(val value: Int) {
    /** Участник был кикнут (исключен) из группы. */
    KICK(0),
    /** Статус участника изменен на Наблюдателя (без прав голоса/текста). */
    OBSERVER(1),
    /** Статус участника изменен на Пользователя (базовые права). */
    USER(2),
    /** Назначение участника Модератором чата. */
    MODERATOR(3);

    companion object {
        /**
         * Возвращает тип модерационного события по целочисленному коду JNI.
         * @param value Целочисленный код JNI.
         * @return Соответствующий элемент [ToxGroupModEvent].
         */
        fun fromInt(value: Int): ToxGroupModEvent =
            values().firstOrNull { it.value == value } ?: USER
    }
}

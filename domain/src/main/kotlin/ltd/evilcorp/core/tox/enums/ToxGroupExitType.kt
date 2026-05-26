package ltd.evilcorp.core.tox.enums

/**
 * Перечисление причин выхода или исключения участника из группового чата NGC.
 * Соответствует кодам `Tox_Group_Exit_Type` в toxcore.
 */
enum class ToxGroupExitType(val value: Int) {
    /** Участник добровольно покинул чат. */
    QUIT(0),
    /** Соединение прервано по таймауту. */
    TIMEOUT(1),
    /** Участник отключился от P2P сети. */
    DISCONNECTED(2),
    /** Собственное отключение от групповой конференции. */
    SELF_DISCONNECTED(3),
    /** Участник был удален (кикнут) модератором. */
    KICK(4),
    /** Ошибка синхронизации данных состояния чата. */
    SYNC_ERROR(5);

    companion object {
        /**
         * Возвращает причину выхода по ее целочисленному коду JNI.
         * @param value Целочисленный код JNI.
         * @return Соответствующий элемент [ToxGroupExitType].
         */
        fun fromInt(value: Int): ToxGroupExitType =
            values().firstOrNull { it.value == value } ?: QUIT
    }
}

package ltd.evilcorp.core.tox.enums

/**
 * Перечисление команд управления передачей файлов (Tox File Control).
 */
enum class ToxFileControl {
    /** Возобновить или начать передачу файла. */
    RESUME,
    /** Приостановить передачу файла. */
    PAUSE,
    /** Отменить или отклонить передачу файла. */
    CANCEL;

    companion object {
        /**
         * Возвращает команду управления по целочисленному коду JNI.
         * @param value Целочисленный код команды.
         * @return Соответствующий элемент [ToxFileControl].
         */
        fun fromInt(value: Int): ToxFileControl = values().getOrElse(value) { CANCEL }
    }
}

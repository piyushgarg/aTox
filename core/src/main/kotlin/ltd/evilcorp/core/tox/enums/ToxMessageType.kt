package ltd.evilcorp.core.tox.enums

/**
 * Перечисление типов отправляемых сообщений по протоколу Tox.
 */
enum class ToxMessageType {
    /** Стандартное текстовое сообщение. */
    NORMAL,
    /** Сообщение-действие (action/slash-команда, например: "/me пошел спать"). */
    ACTION;
 
    /**
     * Конвертирует внутренний тип сообщения Tox во внешнюю доменную модель [ltd.evilcorp.core.model.MessageType].
     */
    fun toMessageType(): ltd.evilcorp.core.model.MessageType = when (this) {
        NORMAL -> ltd.evilcorp.core.model.MessageType.Normal
        ACTION -> ltd.evilcorp.core.model.MessageType.Action
    }

    companion object {
        /**
         * Возвращает тип сообщения по его целочисленному коду JNI.
         * @param value Целочисленный код JNI.
         * @return Соответствующий элемент [ToxMessageType].
         */
        fun fromInt(value: Int): ToxMessageType = values().getOrElse(value) { NORMAL }
    }
}

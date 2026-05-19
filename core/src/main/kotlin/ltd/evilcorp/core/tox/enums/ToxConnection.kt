package ltd.evilcorp.core.tox.enums

import ltd.evilcorp.core.model.ConnectionStatus

/**
 * Перечисление типов сетевого соединения Tox DHT.
 */
enum class ToxConnection {
    /** Соединение отсутствует. */
    NONE,
    /** Соединение по протоколу TCP (через TCP-релеи). */
    TCP,
    /** Прямое UDP соединение с узлами DHT. */
    UDP;

    /**
     * Конвертирует внутренний тип соединения Tox во внешнюю доменную модель [ConnectionStatus].
     */
    fun toConnectionStatus(): ConnectionStatus = when (this) {
        NONE -> ConnectionStatus.None
        TCP -> ConnectionStatus.TCP
        UDP -> ConnectionStatus.UDP
    }

    companion object {
        /**
         * Возвращает элемент перечисления по его целочисленному значению.
         * @param value Целое число, соответствующее типу соединения.
         * @return Соответствующий элемент [ToxConnection].
         */
        fun fromInt(value: Int): ToxConnection = values().getOrElse(value) { NONE }
    }
}

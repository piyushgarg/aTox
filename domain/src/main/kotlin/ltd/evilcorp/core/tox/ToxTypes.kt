package ltd.evilcorp.core.tox

import ltd.evilcorp.core.model.PublicKey

private const val ID_METADATA_LEN = 12

/**
 * Встраиваемый класс (Inline Value Class), представляющий уникальный 76-символьный идентификатор Tox ID.
 * Состоит из 32-байтного публичного ключа пользователя, 4-байтного nospam значения и 2-байтной контрольной суммы.
 */
@JvmInline
value class ToxID(private val value: String) {
    /**
     * Возвращает Tox ID в виде массива байтов.
     */
    fun bytes() = value.hexToBytes()

    /**
     * Возвращает строковое HEX-представление Tox ID.
     */
    fun string() = value

    /**
     * Извлекает публичный ключ (32 байта) из полного Tox ID, отбрасывая служебные метаданные (nospam и контрольную сумму).
     */
    fun toPublicKey() = PublicKey(value.dropLast(ID_METADATA_LEN))

    companion object {
        /**
         * Создает объект ToxID из массива байтов.
         * @param toxId Массив байтов адреса Tox.
         * @return Созданный объект [ToxID].
         */
        fun fromBytes(toxId: ByteArray) = ToxID(toxId.bytesToHex())
    }
}

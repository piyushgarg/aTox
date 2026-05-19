package ltd.evilcorp.core.tox

private const val CHECKSUM_BLOCK_SIZE = 4

/**
 * Валидатор идентификаторов Tox ID.
 * Предоставляет статические методы проверки корректности 76-символьных HEX-строк адресов Tox.
 */
class ToxIdValidator {
    /**
     * Результат валидации Tox ID.
     */
    enum class Result {
        /** Валидация пройдена успешно. */
        NO_ERROR,
        /** Неверная длина строки (ожидается 76 символов). */
        INCORRECT_LENGTH,
        /** Несовпадение контрольной суммы. */
        INVALID_CHECKSUM,
        /** Строка содержит не-HEX символы. */
        NOT_HEX,
    }

    companion object {
        /**
         * Выполняет валидацию переданного идентификатора ToxID.
         * Проверяет формат HEX, длину строки (76 символов) и контрольную сумму по байтовому алгоритму XOR.
         * @param toxID Идентификатор Tox для проверки.
         * @return Результат валидации типа [Result].
         */
        fun validate(toxID: ToxID) = when {
            !toxID.string().matches(Regex("[0-9A-Fa-f]*")) -> Result.NOT_HEX
            toxID.string().length != TOX_ID_LENGTH -> Result.INCORRECT_LENGTH
            toxID.string().chunked(CHECKSUM_BLOCK_SIZE).map {
                it.toInt(radix = 16)
            }.fold(0) { b1, b2 -> b1 xor b2 } != 0 -> Result.INVALID_CHECKSUM
            else -> Result.NO_ERROR
        }
    }
}

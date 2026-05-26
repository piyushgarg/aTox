package ltd.evilcorp.core.tox.save

import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.domain.tox.save.SaveOptions

/**
 * Перечисление возможных статусов загрузки и проверки бинарного файла сохранения Tox (save data).
 */
enum class ToxSaveStatus {
    /** Успешная загрузка и валидация. */
    Ok,
    /** Неверный формат файла сохранения. */
    BadFormat,
    /** Файл зашифрован (требуется пароль). */
    Encrypted,
    /** Ошибка выделения памяти (Out of Memory). */
    OutOfMemory,
    /** Нулевые данные сохранения. */
    Null,
    /** Ошибка выделения портов сокетов. */
    PortAlloc,
    /** Неверно указан хост прокси-сервера. */
    BadProxyHost,
    /** Неверно указан порт прокси-сервера. */
    BadProxyPort,
    /** Неподдерживаемый тип прокси. */
    BadProxyType,
    /** Прокси-сервер не найден или недоступен. */
    ProxyNotFound,
    /** Файл сохранения не найден. */
    SaveNotFound,
}

/**
 * Выполняет тестовую загрузку бинарных данных Tox с расшифровкой (при наличии пароля).
 * Проверяет корректность структуры данных перед инициализацией основного рабочего инстанса.
 * @param options Опции сохранения, содержащие бинарный буфер saveData.
 * @param password Опциональный пароль для расшифровки профиля.
 * @return Статус проверки в виде [ToxSaveStatus].
 */
fun testToxSave(options: SaveOptions, password: String?): ToxSaveStatus {
    val native = NativeTox()
    return try {
        val rawSaveData = options.saveData
        val saveData = if (password != null && rawSaveData != null) {
            val salt = native.getSalt(rawSaveData) ?: return ToxSaveStatus.BadFormat
            val passkey = native.passKeyDeriveWithSalt(password.toByteArray(), salt)
                ?: return ToxSaveStatus.OutOfMemory
            native.passDecrypt(rawSaveData, passkey) ?: return ToxSaveStatus.Encrypted
        } else {
            rawSaveData
        }
        val toxPtr = native.toxNew(saveData)
        if (toxPtr == 0L) {
            return ToxSaveStatus.BadFormat
        }
        native.toxKill(toxPtr)
        ToxSaveStatus.Ok
    } catch (e: Exception) {
        ToxSaveStatus.BadFormat
    }
}

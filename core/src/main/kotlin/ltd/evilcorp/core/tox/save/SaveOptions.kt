package ltd.evilcorp.core.tox.save

/**
 * Типы прокси-серверов, поддерживаемые ядром Tox Core для сетевого трафика.
 */
enum class ProxyType {
    /** Прямое подключение к сети (без прокси). */
    None,
    /** HTTP прокси-сервер. */
    HTTP,
    /** SOCKS5 прокси-сервер (например, Tor на порту 9050). */
    SOCKS5,
}

/**
 * Конфигурационные параметры для инициализации нативной сессии Tox Core.
 * Содержит бинарное состояние профиля и сетевые настройки (UDP, прокси).
 */
@Suppress("ArrayInDataClass")
data class SaveOptions(
    /** Бинарные данные сохранения профиля. Передается null при создании нового пустого профиля. */
    val saveData: ByteArray?,
    /** Флаг включения UDP. Если false, ядро Tox принудительно работает в режиме "только TCP". */
    val udpEnabled: Boolean,
    /** Тип используемого прокси-сервера. */
    val proxyType: ProxyType,
    /** Сетевой адрес хоста прокси-сервера (IP или домен). */
    val proxyAddress: String,
    /** Сетевой порт прокси-сервера. */
    val proxyPort: Int,
)

package ltd.evilcorp.core.tox.bootstrap

/**
 * Интерфейс источника статического JSON-списка серверов бутстрапа Tox.
 */
interface BootstrapNodeJsonSource {
    /**
     * Загружает JSON-строку со списком серверов.
     * @return Текст JSON, либо null, если загрузка не удалась.
     */
    fun load(): String?
}

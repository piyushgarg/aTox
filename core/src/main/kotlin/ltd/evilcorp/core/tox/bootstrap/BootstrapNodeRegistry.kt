package ltd.evilcorp.core.tox.bootstrap

/**
 * Интерфейс реестра серверов бутстрапа (DHT-узлов).
 * Отвечает за предоставление серверов для первичного P2P подключения.
 */
interface BootstrapNodeRegistry {
    /**
     * Возвращает случайный список из [n] узлов бутстрапа.
     * @param n Требуемое количество узлов.
     */
    fun get(n: Int): List<BootstrapNode>

    /**
     * Сбрасывает состояние реестра и перезагружает кэш узлов.
     */
    fun reset()
}

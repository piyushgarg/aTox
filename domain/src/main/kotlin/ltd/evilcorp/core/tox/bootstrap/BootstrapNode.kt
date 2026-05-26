package ltd.evilcorp.core.tox.bootstrap

import ltd.evilcorp.core.model.PublicKey

/**
 * Данные публичного узла DHT (Bootstrap Node).
 * Используется для первоначального подключения клиента к распределенной P2P-сети Tox.
 */
data class BootstrapNode(
    /** IP-адрес или доменное имя узла. */
    val address: String,
    /** Порт UDP/TCP, на котором слушает узел. */
    val port: Int,
    /** 32-байтный нативный публичный ключ узла для шифрования соединения. */
    val publicKey: PublicKey,
)

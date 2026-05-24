package ltd.evilcorp.atox.ui.chat

import ltd.evilcorp.core.model.Message
import ltd.evilcorp.core.model.FileTransfer
import java.util.concurrent.ConcurrentHashMap

object ChatHistoryCache {
    private val cache = ConcurrentHashMap<String, List<Message>>()
    private val transferCache = ConcurrentHashMap<String, List<FileTransfer>>()

    fun put(publicKey: String, messages: List<Message>) {
        cache[publicKey] = messages
    }

    fun get(publicKey: String): List<Message> {
        return cache[publicKey] ?: emptyList()
    }

    fun putTransfers(publicKey: String, transfers: List<FileTransfer>) {
        transferCache[publicKey] = transfers
    }

    fun getTransfers(publicKey: String): List<FileTransfer> {
        return transferCache[publicKey] ?: emptyList()
    }

    fun clear() {
        cache.clear()
        transferCache.clear()
    }
}

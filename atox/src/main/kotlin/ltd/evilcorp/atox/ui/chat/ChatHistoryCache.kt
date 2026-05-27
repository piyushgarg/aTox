package ltd.evilcorp.atox.ui.chat

import android.util.LruCache
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.FileTransfer

object ChatHistoryCache {
    private const val MAX_CACHED_CHATS = 20
    private val cache = LruCache<String, List<Message>>(MAX_CACHED_CHATS)
    private val transferCache = LruCache<String, List<FileTransfer>>(MAX_CACHED_CHATS)

    fun put(publicKey: String, messages: List<Message>) {
        cache.put(publicKey, messages)
    }

    fun get(publicKey: String): List<Message> {
        return cache.get(publicKey) ?: emptyList()
    }

    fun putTransfers(publicKey: String, transfers: List<FileTransfer>) {
        transferCache.put(publicKey, transfers)
    }

    fun getTransfers(publicKey: String): List<FileTransfer> {
        return transferCache.get(publicKey) ?: emptyList()
    }

    fun clear() {
        cache.evictAll()
        transferCache.evictAll()
    }
}

package ltd.evilcorp.domain.features.group

interface IGroupConnectionScheduler {
    fun reconnectAll()
    fun scheduleAutoReconnect(chatId: String, groupNumber: Int)
    fun cancelReconnect(chatId: String)
    fun stopReconnect(chatId: String)
    fun isBootstrapFriend(pk: String): Boolean
}

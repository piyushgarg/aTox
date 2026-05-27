package ltd.evilcorp.domain.feature

interface IGroupConnectionScheduler {
    fun reconnectAll()
    fun scheduleAutoReconnect(chatId: String, groupNumber: Int)
    fun cancelReconnect(chatId: String)
}

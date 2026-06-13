package ltd.evilcorp.domain.fakes

import ltd.evilcorp.domain.features.group.IGroupConnectionScheduler

class FakeGroupConnectionScheduler : IGroupConnectionScheduler {
    var reconnectedAllCalled = false
    var scheduledAutoReconnects = mutableMapOf<String, Int>()
    var cancelledReconnects = mutableListOf<String>()
    var stoppedReconnects = mutableListOf<String>()
    var bootstrapFriends = mutableSetOf<String>()

    override fun reconnectAll() {
        reconnectedAllCalled = true
    }

    override fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {
        scheduledAutoReconnects[chatId] = groupNumber
    }

    override fun cancelReconnect(chatId: String) {
        cancelledReconnects.add(chatId)
    }

    override fun stopReconnect(chatId: String) {
        stoppedReconnects.add(chatId)
    }

    override fun isBootstrapFriend(pk: String): Boolean {
        return bootstrapFriends.contains(pk)
    }
}

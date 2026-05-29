package ltd.evilcorp.atox.infrastructure.tox

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener

/**
 * Coordinates JNI event listener setups.
 * Aggregates EventListenerCallbacks, ToxEventListener, and ToxAvEventListener
 * to decouple listener registration from the primary ToxStarter lifecycle engine.
 */
@Singleton
class ToxListenerCoordinator @Inject constructor(
    private val listenerCallbacks: EventListenerCallbacks,
    val eventListener: ToxEventListener,
    val avEventListener: ToxAvEventListener,
    val databaseUpdater: FriendDatabaseUpdater,
    val notificationDispatcher: FriendNotificationDispatcher,
    val soundManager: FriendSoundManager,
    val sessionCoordinator: FriendSessionCoordinator,
    val groupEventProcessor: GroupEventProcessor,
) {
    fun setupListeners() {
        listenerCallbacks.setUp(eventListener)
        listenerCallbacks.setUp(avEventListener)
    }
}

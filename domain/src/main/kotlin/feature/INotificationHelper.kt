package ltd.evilcorp.domain.feature

import ltd.evilcorp.core.model.PublicKey

interface INotificationHelper {
    fun dismissNotifications(publicKey: PublicKey)
    fun dismissCallNotification(publicKey: PublicKey)
}

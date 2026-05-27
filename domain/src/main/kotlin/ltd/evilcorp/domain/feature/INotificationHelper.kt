package ltd.evilcorp.domain.feature

import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.PublicKey

interface INotificationHelper {
    fun dismissNotifications(publicKey: PublicKey)
    fun dismissCallNotification(publicKey: PublicKey)
    fun showOngoingCallNotification(contact: Contact)
    fun invalidateAvatar(uri: String)
}


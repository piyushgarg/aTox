package ltd.evilcorp.domain.core.network

import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.core.model.PublicKey

interface INotificationHelper {
    fun dismissNotifications(publicKey: PublicKey)
    fun dismissCallNotification(publicKey: PublicKey)
    fun showOngoingCallNotification(contact: Contact)
    fun invalidateAvatar(uri: String)
}


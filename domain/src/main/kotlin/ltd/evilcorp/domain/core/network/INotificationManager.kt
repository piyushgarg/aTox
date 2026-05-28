package ltd.evilcorp.domain.core.network

import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.core.model.PublicKey

interface INotificationManager {
    fun showOngoingCallNotification(contact: Contact)
    fun dismissCallNotification(publicKey: PublicKey)
}

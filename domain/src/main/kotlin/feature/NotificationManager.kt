package ltd.evilcorp.domain.feature

import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.PublicKey

interface NotificationManager {
    fun showOngoingCallNotification(contact: Contact)
    fun dismissCallNotification(publicKey: PublicKey)
}

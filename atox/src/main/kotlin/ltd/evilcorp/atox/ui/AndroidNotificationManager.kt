package ltd.evilcorp.atox.ui

import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.INotificationManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNotificationManager @Inject constructor(
    private val notificationHelper: NotificationHelper
) : INotificationManager {
    override fun showOngoingCallNotification(contact: Contact) = notificationHelper.showOngoingCallNotification(contact)
    override fun dismissCallNotification(publicKey: PublicKey) = notificationHelper.dismissCallNotification(publicKey)
}

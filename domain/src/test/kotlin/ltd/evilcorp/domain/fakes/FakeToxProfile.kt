package ltd.evilcorp.domain.fakes

import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.IToxProfile
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.features.contacts.model.UserStatus

open class FakeToxProfile(
    override val toxId: ToxID = ToxID("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C4ACEE797596D"),
    override val publicKey: PublicKey = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C"),
    override var nospam: Int = 123
) : IToxProfile {
    private var name = "FakeUser"
    private var statusMessage = "FakeStatus"
    private var status = UserStatus.None
    val addedContacts = mutableListOf<Pair<ToxID, String>>()
    val deletedContacts = mutableListOf<PublicKey>()

    override fun getName(): String = name
    override fun setName(name: String) { this.name = name }
    override fun getStatusMessage(): String = statusMessage
    override fun setStatusMessage(statusMessage: String) { this.statusMessage = statusMessage }
    override fun getStatus(): UserStatus = status
    override fun setStatus(status: UserStatus) { this.status = status }
    override fun getContacts(): List<Pair<PublicKey, Int>> = emptyList()
    override fun acceptFriendRequest(publicKey: PublicKey): Result<Unit> = Result.success(Unit)
    override fun addFriendNoRequest(publicKey: PublicKey): Int = 0
    override fun addContact(toxId: ToxID, message: String) {
        addedContacts.add(toxId to message)
    }
    override fun deleteContact(publicKey: PublicKey) {
        deletedContacts.add(publicKey)
    }
    override fun getFriendNumber(publicKey: PublicKey): Int = 0
    override fun getFriendPublicKey(friendNumber: Int): PublicKey? = null
    override fun friendGetLastOnline(publicKey: PublicKey): Long = 0L
}

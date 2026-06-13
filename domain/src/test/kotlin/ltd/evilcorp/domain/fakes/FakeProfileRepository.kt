package ltd.evilcorp.domain.fakes

import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.auth.repository.IProfileRepository
import ltd.evilcorp.domain.features.auth.model.ProfileInfo

class FakeProfileRepository : IProfileRepository {
    var deletedProfilePk: PublicKey? = null
    var databaseCleared = false
    var checkpointCreated = false
    var checkpointCleared = false
    var restoredFromCheckpoint = false

    var checkpointCreateResult = true
    var checkpointRestoreResult = true

    override suspend fun deleteProfile(publicKey: PublicKey) {
        deletedProfilePk = publicKey
    }

    override suspend fun clearDatabase() {
        databaseCleared = true
    }

    override suspend fun createCheckpoint(): Boolean {
        checkpointCreated = true
        return checkpointCreateResult
    }

    override suspend fun restoreFromCheckpoint(): Boolean {
        restoredFromCheckpoint = true
        return checkpointRestoreResult
    }

    override suspend fun clearCheckpoint() {
        checkpointCleared = true
    }

    override suspend fun finalizeProfileCreation(oldId: String, newId: String, name: String) {}

    override fun getActiveProfileId(): String = "default"
    override fun setActiveProfileId(id: String) {}
    override fun getShowProfilePicker(): Boolean = false
    override fun setShowProfilePicker(show: Boolean) {}
    override fun getProfiles(): List<ProfileInfo> = emptyList()
    override fun addOrUpdateProfile(profile: ProfileInfo) {}
    override fun removeProfile(id: String) {}
}

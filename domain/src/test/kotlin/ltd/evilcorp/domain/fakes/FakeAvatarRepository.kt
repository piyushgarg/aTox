package ltd.evilcorp.domain.fakes

import ltd.evilcorp.domain.features.auth.repository.IAvatarRepository

class FakeAvatarRepository : IAvatarRepository {
    var savedBytes: ByteArray? = null
    var saveResult = true
    var fileToReturn: java.io.File = java.io.File("fake_avatar.png")

    override suspend fun saveSelfAvatar(bytes: ByteArray): Boolean {
        savedBytes = bytes
        return saveResult
    }

    override fun getSelfAvatarFile(): java.io.File {
        return fileToReturn
    }
}

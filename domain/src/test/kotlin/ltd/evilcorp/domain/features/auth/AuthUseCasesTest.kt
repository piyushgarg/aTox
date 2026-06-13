package ltd.evilcorp.domain.features.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.auth.usecase.*
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.transfer.FileStoragePlatformCoordinator
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.FileTransferRepositories
import ltd.evilcorp.domain.fakes.*
import kotlin.test.*

class AuthUseCasesTest {

    private val userRepo = FakeUserRepository()
    private val toxProfile = FakeToxProfile()
    private val userManager = UserManager(userRepo, toxProfile)

    @Test
    fun `CreateProfileUseCase delegating to UserManager`() = runTest {
        val useCase = CreateProfileUseCase(userManager)
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val user = User(pk.string(), "Alice", "Hello")

        useCase.execute(user)

        val retrieved = userRepo.get(pk.string()).first()
        assertEquals(user, retrieved)
    }

    @Test
    fun `VerifyProfileExistsUseCase delegating to UserManager`() = runTest {
        val useCase = VerifyProfileExistsUseCase(userManager)
        val pk = toxProfile.publicKey
        toxProfile.setName("Alice")
        toxProfile.setStatusMessage("Active")

        useCase.execute(pk)

        val retrieved = userRepo.get(pk.string()).first()
        assertEquals("Alice", retrieved?.name)
        assertEquals("Active", retrieved?.statusMessage)
    }

    @Test
    fun `GetSelfUserUseCase retrieves self user and details`() = runTest {
        val tox = FakeTox()
        val useCase = GetSelfUserUseCase(userManager, tox, FakeProfileRepository())

        assertEquals(tox.publicKey, useCase.publicKey)
        assertEquals(tox.toxId, useCase.toxId)

        val selfUser = User(tox.publicKey.string(), "Self", "Status")
        userRepo.add(selfUser)

        val retrieved = useCase.execute().first()
        assertEquals(selfUser, retrieved)
    }

    @Test
    fun `InitializeToxUseCase calls IToxStarter`() {
        val toxStarter = object : ltd.evilcorp.domain.core.network.IToxStarter {
            var loadedPassword: String? = null
            var stopped = false
            var startedWithSave: ByteArray? = null
            override fun tryLoadTox(password: String?): ToxSaveStatus {
                loadedPassword = password
                return ToxSaveStatus.Ok
            }
            override fun stopTox() {
                stopped = true
            }
            override fun startTox(save: ByteArray?, password: String?): ToxSaveStatus {
                startedWithSave = save
                return ToxSaveStatus.Ok
            }
        }
        val useCase = InitializeToxUseCase(toxStarter)
        val status = useCase.execute("secret_pass")

        assertEquals(ToxSaveStatus.Ok, status)
        assertEquals("secret_pass", toxStarter.loadedPassword)
    }

    @Test
    fun `ClearDatabaseUseCase calls IProfileRepository`() = runTest {
        val profileRepo = FakeProfileRepository()
        val useCase = ClearDatabaseUseCase(profileRepo)

        useCase.execute()
        assertTrue(profileRepo.databaseCleared)
    }

    @Test
    fun `DeleteProfileUseCase calls IProfileRepository`() = runTest {
        val profileRepo = FakeProfileRepository()
        val useCase = DeleteProfileUseCase(toxProfile, profileRepo, Dispatchers.Unconfined)

        useCase.execute()
        assertEquals(toxProfile.publicKey, profileRepo.deletedProfilePk)
    }

    @Test
    fun `GetSelfAvatarUseCase retrieves avatar file`() {
        val avatarRepo = FakeAvatarRepository()
        val file = java.io.File("custom_avatar.png")
        avatarRepo.fileToReturn = file
        val useCase = GetSelfAvatarUseCase(avatarRepo)

        val result = useCase.execute()
        assertEquals(file, result)
    }

    @Test
    fun `GetSelfAvatarUriUseCase returns URI string if file exists`() {
        val avatarRepo = FakeAvatarRepository()
        val tempFile = java.io.File.createTempFile("avatar_exists", ".png").apply { deleteOnExit() }
        avatarRepo.fileToReturn = tempFile

        val getSelfAvatarUseCase = GetSelfAvatarUseCase(avatarRepo)
        val useCase = GetSelfAvatarUriUseCase(getSelfAvatarUseCase)

        val uri = useCase.execute()
        assertEquals(tempFile.toURI().toString(), uri)
    }

    @Test
    fun `GetSelfAvatarUriUseCase returns null if file does not exist`() {
        val avatarRepo = FakeAvatarRepository()
        avatarRepo.fileToReturn = java.io.File("non_existent_avatar_file.png")

        val getSelfAvatarUseCase = GetSelfAvatarUseCase(avatarRepo)
        val useCase = GetSelfAvatarUriUseCase(getSelfAvatarUseCase)

        val uri = useCase.execute()
        assertNull(uri)
    }

    @Test
    fun `UpdateUserProfileUseCase updates Name, StatusMessage, and Status`() = runTest {
        val useCase = UpdateUserProfileUseCase(userManager)
        val pk = toxProfile.publicKey
        userRepo.add(User(pk.string(), "Alice", "Hello", UserStatus.None))

        useCase.execute(ProfileAction.Name("Alice Updated"))
        assertEquals("Alice Updated", toxProfile.getName())

        useCase.execute(ProfileAction.StatusMessage("Hello Updated"))
        assertEquals("Hello Updated", toxProfile.getStatusMessage())

        useCase.execute(ProfileAction.Status(UserStatus.Busy))
        assertEquals(UserStatus.Busy, toxProfile.getStatus())
    }

    @Test
    fun `ManageProfileCheckpointUseCase executes Create, Clear, and Restore actions`() = runTest {
        val profileRepo = FakeProfileRepository()
        val useCase = ManageProfileCheckpointUseCase(profileRepo)

        val createResult = useCase.execute(CheckpointAction.Create)
        assertTrue(createResult)
        assertTrue(profileRepo.checkpointCreated)

        val clearResult = useCase.execute(CheckpointAction.Clear)
        assertTrue(clearResult)
        assertTrue(profileRepo.checkpointCleared)

        val restoreResult = useCase.execute(CheckpointAction.Restore)
        assertTrue(restoreResult)
        assertTrue(profileRepo.restoredFromCheckpoint)
    }

    @Test
    fun `SaveAvatarUseCase saves avatar and broadcasts if successful`() = runTest {
        val avatarRepo = FakeAvatarRepository()
        
        val fakeTox = FakeTox()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val platformHelper = FakeFileTransferPlatformHelper()
        val storageHelper = FakeFileStorageHelper()
        val storageCoordinator = FileStoragePlatformCoordinator(platformHelper, storageHelper)
        
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val transferRepo = FakeFileTransferRepository()
        val settingsRepo = FakeUserSettingsRepository()
        val repositories = FileTransferRepositories(contactRepo, messageRepo, transferRepo, settingsRepo)
        
        val sessionRegistry = FakeFileTransferSessionRegistry()
        
        val fileTransferManager = FileTransferManager(
            scope,
            storageCoordinator,
            repositories,
            fakeTox,
            fakeTox,
            sessionRegistry,
            Dispatchers.Unconfined
        )

        val useCase = SaveAvatarUseCase(avatarRepo, fileTransferManager, Dispatchers.Unconfined)
        val bytes = byteArrayOf(1, 2, 3)

        val success = useCase.execute(bytes)
        assertTrue(success)
        assertNotNull(avatarRepo.savedBytes)
        assertArrayEquals(bytes, avatarRepo.savedBytes)
    }

    @Test
    fun `BroadcastAvatarUseCase broadcasts avatar via FileTransferManager`() = runTest {
        val fakeTox = FakeTox()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val platformHelper = FakeFileTransferPlatformHelper()
        val storageHelper = FakeFileStorageHelper()
        val storageCoordinator = FileStoragePlatformCoordinator(platformHelper, storageHelper)
        
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val transferRepo = FakeFileTransferRepository()
        val settingsRepo = FakeUserSettingsRepository()
        val repositories = FileTransferRepositories(contactRepo, messageRepo, transferRepo, settingsRepo)
        
        val sessionRegistry = FakeFileTransferSessionRegistry()
        
        val fileTransferManager = FileTransferManager(
            scope,
            storageCoordinator,
            repositories,
            fakeTox,
            fakeTox,
            sessionRegistry,
            Dispatchers.Unconfined
        )

        val useCase = BroadcastAvatarUseCase(fileTransferManager)
        useCase.execute()
        // No exception indicates successful broadcast call integration
    }

    private fun assertArrayEquals(expected: ByteArray, actual: ByteArray?) {
        assertNotNull(actual)
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) {
            assertEquals(expected[i], actual[i])
        }
    }
}

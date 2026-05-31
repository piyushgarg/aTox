package ltd.evilcorp.core.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.dao.FakeFriendRequestDao
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FriendRequestRepositoryImplTest {

    private lateinit var dao: FakeFriendRequestDao
    private lateinit var repository: FriendRequestRepositoryImpl

    private val testRequest = FriendRequest(
        publicKey = "abcd1234",
        message = "Please add me as a friend on aTox!"
    )

    @BeforeTest
    fun setUp() {
        dao = FakeFriendRequestDao()
        repository = FriendRequestRepositoryImpl(dao)
    }

    @Test
    fun testAdd_and_get() = runTest {
        repository.add(testRequest)
        val loaded = repository.get(testRequest.publicKey).first()
        assertEquals(testRequest, loaded)
    }

    @Test
    fun testGet_nonExistent_returnsNull() = runTest {
        val loaded = repository.get("non-existent").first()
        assertNull(loaded)
    }

    @Test
    fun testGetAll_returnsAllRequests() = runTest {
        val second = testRequest.copy(publicKey = "9999", message = "Add me too!")
        repository.add(testRequest)
        repository.add(second)

        val list = repository.getAll().first()
        assertEquals(2, list.size)
        assertTrue(list.contains(testRequest))
        assertTrue(list.contains(second))
    }

    @Test
    fun testDelete_removesRequest() = runTest {
        repository.add(testRequest)
        assertEquals(1, repository.count())

        repository.delete(testRequest)
        assertEquals(0, repository.count())
        assertNull(repository.get(testRequest.publicKey).first())
    }

    @Test
    fun testCount_returnsCorrectCount() = runTest {
        assertEquals(0, repository.count())
        repository.add(testRequest)
        assertEquals(1, repository.count())
    }
}

package ltd.evilcorp.core.platform.storage

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupInvite
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupSessionRegistryImplTest {

    private lateinit var registry: GroupSessionRegistryImpl

    @BeforeTest
    fun setUp() {
        registry = GroupSessionRegistryImpl()
    }

    @Test
    fun testInitialStates() = runTest {
        assertEquals("", registry.activeGroup)
        assertNull(registry.pendingInvite.value)
        assertTrue(registry.connectionStatuses.value.isEmpty())
    }

    @Test
    fun testSetActiveGroup() {
        registry.activeGroup = "group123"
        assertEquals("group123", registry.activeGroup)
    }

    @Test
    fun testSetPendingInvite() = runTest {
        val invite = GroupInvite(
            friendNo = 5,
            inviteData = "invite_data_bytes".toByteArray(),
            groupName = "Awesome Group"
        )
        registry.setPendingInvite(invite)
        assertEquals(invite, registry.pendingInvite.first())

        registry.setPendingInvite(null)
        assertNull(registry.pendingInvite.first())
    }

    @Test
    fun testSetAndRemoveConnectionStatuses() = runTest {
        registry.setConnectionStatus("chat1", GroupConnectionStatus.Connected)
        registry.setConnectionStatus("chat2", GroupConnectionStatus.Connecting)

        val statuses = registry.connectionStatuses.first()
        assertEquals(2, statuses.size)
        assertEquals(GroupConnectionStatus.Connected, statuses["chat1"])
        assertEquals(GroupConnectionStatus.Connecting, statuses["chat2"])

        registry.removeConnectionStatus("chat1")
        val updatedStatuses = registry.connectionStatuses.first()
        assertEquals(1, updatedStatuses.size)
        assertNull(updatedStatuses["chat1"])
        assertEquals(GroupConnectionStatus.Connecting, updatedStatuses["chat2"])
    }
}

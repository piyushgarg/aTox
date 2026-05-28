// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.contactlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.infrastructure.tox.ToxStarter
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.contacts.ContactManager
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.contacts.FriendRequestManager
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.contacts.usecase.DeleteContactUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import ltd.evilcorp.atox.infrastructure.sharing.SharedContentManager
import ltd.evilcorp.atox.SharedContent

import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val chatManager: ChatManager,
    private val contactManager: ContactManager,
    private val fileTransferManager: FileTransferManager,
    private val groupManager: GroupManager,
    private val friendRequestManager: FriendRequestManager,
    private val tox: ITox,
    private val settings: Settings,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val sharedContentManager: SharedContentManager,
    userManager: UserManager,
) : ViewModel() {
    val sharedContent: StateFlow<SharedContent?> = sharedContentManager.sharedContent

    fun clearSharedContent() {
        sharedContentManager.clear()
    }
    val publicKey by lazy { tox.publicKey }

    val user: StateFlow<User?> by lazy {
        userManager.get(publicKey)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val groupInvite: StateFlow<GroupInvite?> = groupManager.pendingInvite
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val groupInviteFriendName: StateFlow<String> = groupManager.pendingInvite
        .map { invite ->
            if (invite == null) return@map ""
            val pk = tox.getFriendPublicKey(invite.friendNo) ?: return@map "Friend #${invite.friendNo}"
            contactManager.get(pk).firstOrNull()?.name ?: pk.string().take(8)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun acceptGroupInvite() {
        viewModelScope.launch {
            groupManager.acceptInvite()
        }
    }

    fun declineGroupInvite() {
        groupManager.declineInvite()
    }

    init {
        if (tox.started) {
            viewModelScope.launch {
                groupManager.reconnectAll()
            }
        }
    }

    private val _selectedChatSnapshot = MutableStateFlow<Contact?>(null)
    val selectedChatSnapshot = _selectedChatSnapshot.asStateFlow()

    fun clearSelectedChat() {
        _selectedChatSnapshot.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun prepareOpenChat(contact: Contact) {
        _selectedChatSnapshot.value = contact
    }

    val contacts: StateFlow<List<Contact>> = contactManager.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredContacts: StateFlow<List<Contact>> = contacts
        .combine(searchQuery) { list, query ->
            if (query.isBlank()) emptyList()
            else list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.publicKey.contains(query, ignoreCase = true)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val attentionCount: StateFlow<Int> = contacts
        .combine(friendRequestManager.getAll().onStart { emit(emptyList()) }) { contactsList, requestsList ->
            ltd.evilcorp.atox.ui.contactlist.components.chatListAttentionCount(contactsList, requestsList)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private val debouncedSearchQuery = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val visibleContacts: StateFlow<List<Contact>> = contactManager.getAll()
        .combine(debouncedSearchQuery) { contactsList, query ->
            ltd.evilcorp.atox.ui.contactlist.components.visibleChatContacts(contactsList, query)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun isToxRunning() = tox.started

    fun quittingNeedsConfirmation(): Boolean = settings.confirmQuitting

    fun deleteContact(publicKey: PublicKey) {
        viewModelScope.launch {
            deleteContactUseCase.execute(publicKey)
        }
    }

    suspend fun contactAdded(toxId: PublicKey): Boolean {
        return contactManager.get(toxId).firstOrNull() != null
    }

    fun onShareText(what: String, to: Contact) {
        viewModelScope.launch {
            chatManager.sendMessage(PublicKey(to.publicKey), what)
        }
    }

    fun onShareFile(uri: android.net.Uri, to: Contact) {
        viewModelScope.launch {
            fileTransferManager.create(PublicKey(to.publicKey), uri.toString())
        }
    }
}

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
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.chat.usecase.SendChatMessageUseCase
import ltd.evilcorp.domain.features.transfer.usecase.ManageFileTransferUseCase
import ltd.evilcorp.domain.features.transfer.usecase.FileTransferAction
import ltd.evilcorp.domain.features.chat.model.MessageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import ltd.evilcorp.atox.infrastructure.sharing.SharedContentRegistry
import ltd.evilcorp.atox.SharedContent
import ltd.evilcorp.domain.features.contacts.usecase.DeleteContactUseCase
import ltd.evilcorp.domain.features.group.usecase.AcceptGroupInviteUseCase
import ltd.evilcorp.domain.features.group.usecase.DeclineGroupInviteUseCase
import ltd.evilcorp.domain.features.contacts.usecase.GetContactsUseCase
import ltd.evilcorp.domain.features.contacts.usecase.GetFriendRequestsUseCase
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.group.usecase.GetGroupInviteUseCase
import ltd.evilcorp.domain.features.group.usecase.ReconnectGroupsUseCase
import ltd.evilcorp.domain.features.contacts.usecase.GetFriendPublicKeyUseCase
import ltd.evilcorp.domain.features.contacts.usecase.GetContactUseCase
import ltd.evilcorp.domain.features.settings.usecase.GetToxRunningStateUseCase
import ltd.evilcorp.domain.features.settings.usecase.GetUserSettingsUseCase

import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val getGroupInviteUseCase: GetGroupInviteUseCase,
    private val reconnectGroupsUseCase: ReconnectGroupsUseCase,
    private val getFriendRequestsUseCase: GetFriendRequestsUseCase,
    private val getSelfUserUseCase: GetSelfUserUseCase,
    private val getToxRunningStateUseCase: GetToxRunningStateUseCase,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val getFriendPublicKeyUseCase: GetFriendPublicKeyUseCase,
    private val getContactUseCase: GetContactUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val acceptGroupInviteUseCase: AcceptGroupInviteUseCase,
    private val declineGroupInviteUseCase: DeclineGroupInviteUseCase,
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val manageFileTransferUseCase: ManageFileTransferUseCase,
    private val sharedContentRegistry: SharedContentRegistry,
) : ViewModel() {
    val sharedContent: StateFlow<SharedContent?> = sharedContentRegistry.sharedContent

    fun clearSharedContent() {
        sharedContentRegistry.clear()
    }
    val publicKey by lazy { getSelfUserUseCase.publicKey }

    val user: StateFlow<User?> by lazy {
        getSelfUserUseCase.execute()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val groupInvite: StateFlow<GroupInvite?> = getGroupInviteUseCase.execute()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val groupInviteFriendName: StateFlow<String> = getGroupInviteUseCase.execute()
        .map { invite ->
            if (invite == null) return@map ""
            val pk = getFriendPublicKeyUseCase.execute(invite.friendNo) ?: return@map "Friend #${invite.friendNo}"
            getContactUseCase.execute(pk).firstOrNull()?.name ?: pk.string().take(8)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun acceptGroupInvite() {
        viewModelScope.launch {
            acceptGroupInviteUseCase.execute()
        }
    }

    fun declineGroupInvite() {
        declineGroupInviteUseCase.execute()
    }

    init {
        if (getToxRunningStateUseCase.execute()) {
            reconnectGroupsUseCase.execute()
        }
    }

    private val _selectedChatSnapshot = MutableStateFlow<Contact?>(null)
    val selectedChatSnapshot = _selectedChatSnapshot.asStateFlow()

    private val _selectedGroupSnapshot = MutableStateFlow<Group?>(null)
    val selectedGroupSnapshot = _selectedGroupSnapshot.asStateFlow()

    fun clearSelectedChat() {
        _selectedChatSnapshot.value = null
    }

    fun clearSelectedGroup() {
        _selectedGroupSnapshot.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun prepareOpenChat(contact: Contact) {
        _selectedChatSnapshot.value = contact
        _selectedGroupSnapshot.value = null
    }

    fun prepareOpenGroup(group: Group) {
        _selectedGroupSnapshot.value = group
        _selectedChatSnapshot.value = null
    }

    val contacts: StateFlow<List<Contact>> = getContactsUseCase.execute()
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
        .combine(getFriendRequestsUseCase.execute().onStart { emit(emptyList()) }) { contactsList, requestsList ->
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
    val visibleContacts: StateFlow<List<Contact>> = getContactsUseCase.execute()
        .combine(debouncedSearchQuery) { contactsList, query ->
            ltd.evilcorp.atox.ui.contactlist.components.visibleChatContacts(contactsList, query)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun isToxRunning() = getToxRunningStateUseCase.execute()

    fun quittingNeedsConfirmation(): Boolean = getUserSettingsUseCase.settings.value.confirmQuitting

    fun deleteContact(publicKey: PublicKey) {
        viewModelScope.launch {
            deleteContactUseCase.execute(publicKey)
        }
    }

    suspend fun contactAdded(toxId: PublicKey): Boolean {
        return getContactUseCase.execute(toxId).firstOrNull() != null
    }

    fun onShareText(what: String, to: Contact) {
        viewModelScope.launch {
            sendChatMessageUseCase.execute(PublicKey(to.publicKey), what, MessageType.Normal)
        }
    }

    fun onShareFile(uri: android.net.Uri, to: Contact) {
        viewModelScope.launch {
            manageFileTransferUseCase.execute(FileTransferAction.Create(PublicKey(to.publicKey), uri.toString()))
        }
    }
}

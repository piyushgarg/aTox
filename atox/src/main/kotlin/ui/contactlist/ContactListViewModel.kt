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
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.tox.ToxStarter
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.User
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.feature.ChatManager
import ltd.evilcorp.domain.feature.ContactManager
import ltd.evilcorp.domain.feature.FileTransferManager
import ltd.evilcorp.domain.feature.FriendRequestManager
import ltd.evilcorp.domain.feature.GroupManager
import ltd.evilcorp.domain.feature.GroupInvite
import ltd.evilcorp.domain.feature.UserManager
import ltd.evilcorp.core.tox.save.ProxyType
import ltd.evilcorp.core.tox.save.SaveOptions
import ltd.evilcorp.core.tox.save.testToxSave
import ltd.evilcorp.domain.tox.Tox
import ltd.evilcorp.core.tox.save.ToxSaveStatus
import ltd.evilcorp.atox.domain.usecase.DeleteProfileUseCase

import ltd.evilcorp.atox.domain.usecase.DeleteContactUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map

class ContactListViewModel @Inject constructor(
    private val callManager: CallManager,
    private val chatManager: ChatManager,
    private val contactManager: ContactManager,
    private val fileTransferManager: FileTransferManager,
    private val groupManager: GroupManager,
    private val notificationHelper: NotificationHelper,
    private val tox: Tox,
    private val settings: Settings,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    userManager: UserManager,
) : ViewModel() {
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

    val groups: StateFlow<List<Group>> = groupManager.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
        groupManager.acceptInvite()
    }

    fun declineGroupInvite() {
        groupManager.declineInvite()
    }

    init {
        if (tox.started) {
            groupManager.reconnectAll()
        }
    }

    private val _selectedChatSnapshot = MutableStateFlow<Contact?>(null)
    val selectedChatSnapshot = _selectedChatSnapshot.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun prepareOpenChat(contact: Contact) {
        _selectedChatSnapshot.value = contact
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messagesList = chatManager.messagesFor(PublicKey(contact.publicKey)).first()
                ltd.evilcorp.atox.ui.chat.ChatHistoryCache.put(contact.publicKey, messagesList.takeLast(15))
                
                val transfersList = fileTransferManager.transfersFor(PublicKey(contact.publicKey)).first()
                ltd.evilcorp.atox.ui.chat.ChatHistoryCache.putTransfers(contact.publicKey, transfersList)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    val contacts: StateFlow<List<Contact>> = contactManager.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val visibleContacts: StateFlow<List<Contact>> = contactManager.getAll()
        .combine(_searchQuery) { contactsList, query ->
            ltd.evilcorp.atox.ui.contactlist.components.visibleChatContacts(contactsList, query)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun isToxRunning() = tox.started

    fun deleteProfileAndData() {
        viewModelScope.launch {
            deleteProfileUseCase.execute()
        }
    }

    fun quittingNeedsConfirmation(): Boolean = settings.confirmQuitting

    fun deleteContact(publicKey: PublicKey) {
        viewModelScope.launch {
            deleteContactUseCase.execute(publicKey)
        }
    }

    suspend fun contactAdded(toxId: PublicKey): Boolean {
        return contactManager.get(toxId).firstOrNull() != null
    }

    fun onShareText(what: String, to: Contact) = chatManager.sendMessage(PublicKey(to.publicKey), what)

    fun onShareFile(uri: android.net.Uri, to: Contact) {
        viewModelScope.launch {
            fileTransferManager.create(PublicKey(to.publicKey), uri)
        }
    }
}

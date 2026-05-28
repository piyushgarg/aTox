// SPDX-FileCopyrightText: 2019-2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.addcontact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.tox.ToxStarter
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.ContactManager
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus

import ltd.evilcorp.domain.features.contacts.usecase.AddContactUseCase
import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.features.auth.model.User

import dagger.hilt.android.lifecycle.HiltViewModel

private const val MIN_TOX_ID_LENGTH = 64

@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val addContactUseCase: AddContactUseCase,
    private val contactManager: ContactManager,
    private val tox: ITox,
    private val toxStarter: ToxStarter,
    private val userManager: UserManager,
) : ViewModel() {
    val publicKey by lazy { tox.publicKey }
    val user: StateFlow<User?> = userManager.get(publicKey)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val toxId by lazy { tox.toxId }
    val contacts: StateFlow<List<Contact>> = contactManager.getAll()
        .map { list -> list }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorResId = MutableStateFlow<Int?>(null)
    val errorResId = _errorResId.asStateFlow()

    private val _uiEvents = MutableSharedFlow<AddContactUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    sealed interface AddContactUiEvent {
        object Success : AddContactUiEvent
        data class ShowError(val errorResId: Int) : AddContactUiEvent
    }

    fun isToxRunning() = tox.started
    fun tryLoadTox(): Boolean = toxStarter.tryLoadTox(null) == ToxSaveStatus.Ok

    fun addContact(toxIdStr: String, message: String) {
        val trimmedToxId = toxIdStr.trim()
        if (trimmedToxId.length < MIN_TOX_ID_LENGTH) {
            _errorResId.value = R.string.add_contact_error_invalid
            return
        }

        _errorResId.value = null

        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                addContactUseCase.execute(ToxID(trimmedToxId), message)
                _uiEvents.emit(AddContactUiEvent.Success)
            }.onFailure {
                _errorResId.value = R.string.create_profile_error_failed
            }
            _isLoading.value = false
        }
    }
}

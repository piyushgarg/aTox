// SPDX-FileCopyrightText: 2019-2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.addcontact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.features.contacts.usecase.AddContactUseCase
import ltd.evilcorp.domain.features.contacts.usecase.GetContactsUseCase
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.settings.usecase.ManageToxLifecycleUseCase
import ltd.evilcorp.domain.features.auth.model.User

import dagger.hilt.android.lifecycle.HiltViewModel

private const val MIN_TOX_ID_LENGTH = 64

@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val addContactUseCase: AddContactUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val getSelfUserUseCase: GetSelfUserUseCase,
    private val manageToxLifecycleUseCase: ManageToxLifecycleUseCase,
) : ViewModel() {
    val publicKey by lazy { getSelfUserUseCase.publicKey }
    val user: StateFlow<User?> = getSelfUserUseCase.execute()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val toxId by lazy { getSelfUserUseCase.toxId }
    val contacts: StateFlow<List<Contact>> = getContactsUseCase.execute()
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

    fun isToxRunning() = manageToxLifecycleUseCase.started

    fun tryLoadTox(): Boolean {
        viewModelScope.launch {
            manageToxLifecycleUseCase.execute(ltd.evilcorp.domain.features.settings.usecase.ToxLifecycleAction.TryLoad(null))
        }
        return true
    }

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

// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.contactprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.usecase.GetContactUseCase
import ltd.evilcorp.domain.core.model.PublicKey

private const val FLOW_SUBSCRIBE_TIMEOUT_MS = 5000L

@HiltViewModel
class ContactProfileViewModel @Inject constructor(
    private val getContactUseCase: GetContactUseCase,
) : ViewModel() {

    private val _contact = MutableStateFlow<Contact?>(null)
    val contact: StateFlow<Contact?> = _contact.asStateFlow()

    fun loadContact(publicKey: String) {
        viewModelScope.launch {
            getContactUseCase.execute(PublicKey(publicKey))
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(FLOW_SUBSCRIBE_TIMEOUT_MS),
                    initialValue = null
                )
                .collect { contact ->
                    _contact.value = contact
                }
        }
    }
}

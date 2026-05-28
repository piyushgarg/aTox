// SPDX-FileCopyrightText: 2020 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.userprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.broadcastAvatar
import ltd.evilcorp.domain.features.auth.repository.IAvatarRepository
import ltd.evilcorp.domain.features.auth.usecase.SaveAvatarUseCase
import ltd.evilcorp.domain.features.auth.usecase.DeleteProfileUseCase
import java.io.File

private const val DEBOUNCE_DELAY_MS = 800L

sealed interface AvatarCropUiState {
    object Idle : AvatarCropUiState
    object Processing : AvatarCropUiState
    object Success : AvatarCropUiState
    object Failure : AvatarCropUiState
}

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userManager: UserManager,
    private val tox: ITox,
    private val fileTransferManager: FileTransferManager,
    private val avatarRepository: IAvatarRepository,
    private val saveAvatarUseCase: SaveAvatarUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
) : ViewModel() {

    fun deleteProfileAndData() {
        viewModelScope.launch {
            deleteProfileUseCase.execute()
        }
    }

    val publicKey by lazy { tox.publicKey }
    val toxId by lazy { tox.toxId }
    val user: StateFlow<User?> = userManager.get(publicKey)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _avatarFile = MutableStateFlow<File?>(null)
    val avatarFile: StateFlow<File?> = _avatarFile.asStateFlow()

    private val nameUpdates = MutableSharedFlow<String>()
    private val statusUpdates = MutableSharedFlow<String>()

    private val _cropState = MutableStateFlow<AvatarCropUiState>(AvatarCropUiState.Idle)
    val cropState = _cropState.asStateFlow()

    init {
        loadAvatar()

        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            nameUpdates.debounce(DEBOUNCE_DELAY_MS).collectLatest { name ->
                withContext(Dispatchers.IO) {
                    userManager.setName(name)
                }
            }
        }

        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            statusUpdates.debounce(DEBOUNCE_DELAY_MS).collectLatest { status ->
                withContext(Dispatchers.IO) {
                    userManager.setStatusMessage(status)
                }
            }
        }
    }

    fun loadAvatar() {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val f = avatarRepository.getSelfAvatarFile()
                if (f.exists() && f.length() > 0L) f else null
            }
            _avatarFile.value = file
        }
    }

    fun setName(name: String) {
        viewModelScope.launch {
            nameUpdates.emit(name)
        }
    }

    fun setStatusMessage(statusMessage: String) {
        viewModelScope.launch {
            statusUpdates.emit(statusMessage)
        }
    }

    fun setStatus(status: UserStatus) {
        viewModelScope.launch {
            userManager.setStatus(status)
        }
    }

    fun broadcastAvatar() {
        viewModelScope.launch {
            fileTransferManager.broadcastAvatar()
        }
    }

    fun resetCropState() {
        _cropState.value = AvatarCropUiState.Idle
    }

    fun saveAvatar(avatarBytes: ByteArray) {
        viewModelScope.launch {
            _cropState.value = AvatarCropUiState.Processing
            val success = saveAvatarUseCase.execute(avatarBytes)
            if (success) {
                loadAvatar()
            }
            _cropState.value = if (success) AvatarCropUiState.Success else AvatarCropUiState.Failure
        }
    }
}

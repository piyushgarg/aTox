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
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.auth.usecase.UpdateUserProfileUseCase
import ltd.evilcorp.domain.features.auth.usecase.ProfileAction
import ltd.evilcorp.domain.features.auth.usecase.BroadcastAvatarUseCase
import ltd.evilcorp.domain.features.auth.usecase.GetSelfAvatarUseCase
import ltd.evilcorp.domain.features.auth.usecase.SaveAvatarUseCase
import ltd.evilcorp.domain.features.auth.usecase.DeleteProfileUseCase
import java.io.File
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory

private const val DEBOUNCE_DELAY_MS = 800L

sealed interface AvatarCropUiState {
    object Idle : AvatarCropUiState
    object Processing : AvatarCropUiState
    object Success : AvatarCropUiState
    object Failure : AvatarCropUiState
}

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val getSelfUserUseCase: GetSelfUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val broadcastAvatarUseCase: BroadcastAvatarUseCase,
    private val getSelfAvatarUseCase: GetSelfAvatarUseCase,
    private val saveAvatarUseCase: SaveAvatarUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
) : ViewModel() {

    fun deleteProfileAndData() {
        viewModelScope.launch {
            deleteProfileUseCase.execute()
        }
    }

    val publicKey by lazy { getSelfUserUseCase.publicKey }
    val toxId by lazy { getSelfUserUseCase.toxId }
    val user: StateFlow<User?> = getSelfUserUseCase.execute()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _avatarFile = MutableStateFlow<File?>(null)
    val avatarFile: StateFlow<File?> = _avatarFile.asStateFlow()

    private val _avatarBitmap = MutableStateFlow<ImageBitmap?>(null)
    val avatarBitmap: StateFlow<ImageBitmap?> = _avatarBitmap.asStateFlow()

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
                    updateUserProfileUseCase.execute(ProfileAction.Name(name))
                }
            }
        }

        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            statusUpdates.debounce(DEBOUNCE_DELAY_MS).collectLatest { status ->
                withContext(Dispatchers.IO) {
                    updateUserProfileUseCase.execute(ProfileAction.StatusMessage(status))
                }
            }
        }
    }

    fun loadAvatar() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val f = getSelfAvatarUseCase.execute()
                if (f.exists() && f.length() > 0L) {
                    try {
                        BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }
            _avatarBitmap.value = result
            _avatarFile.value = if (result != null) getSelfAvatarUseCase.execute() else null
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
            updateUserProfileUseCase.execute(ProfileAction.Status(status))
        }
    }

    fun broadcastAvatar() {
        viewModelScope.launch {
            broadcastAvatarUseCase.execute()
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

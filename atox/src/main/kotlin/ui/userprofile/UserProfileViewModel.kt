// SPDX-FileCopyrightText: 2020 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.userprofile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.model.toDb
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.core.model.User
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.domain.feature.UserManager
import ltd.evilcorp.domain.tox.Tox
import ltd.evilcorp.domain.feature.FileTransferManager
import java.io.File

sealed interface AvatarCropUiState {
    object Idle : AvatarCropUiState
    object Processing : AvatarCropUiState
    object Success : AvatarCropUiState
    object Failure : AvatarCropUiState
}

class UserProfileViewModel @Inject constructor(
    private val context: Context,
    private val userManager: UserManager,
    private val tox: Tox,
    private val fileTransferManager: FileTransferManager
) : ViewModel() {
    val publicKey by lazy { tox.publicKey }
    val toxId by lazy { tox.toxId }
    val user: StateFlow<User?> = userManager.get(publicKey)
        .map { it?.toDb() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _avatar = MutableStateFlow<Bitmap?>(null)
    val avatar: StateFlow<Bitmap?> = _avatar.asStateFlow()

    init {
        loadAvatar()
    }

    fun loadAvatar() {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                val file = File(context.filesDir, "self_avatar.png")
                if (file.exists() && file.length() > 0L) {
                    try {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }
            _avatar.value = bmp
        }
    }

    fun setName(name: String) = userManager.setName(name)
    fun setStatusMessage(statusMessage: String) = userManager.setStatusMessage(statusMessage)
    fun setStatus(status: UserStatus) = userManager.setStatus(status)

    fun broadcastAvatar() {
        viewModelScope.launch {
            fileTransferManager.broadcastAvatar()
        }
    }

    private val _cropState = MutableStateFlow<AvatarCropUiState>(AvatarCropUiState.Idle)
    val cropState = _cropState.asStateFlow()

    fun resetCropState() {
        _cropState.value = AvatarCropUiState.Idle
    }

    fun cropAndSaveAvatar(
        originalBitmap: Bitmap,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        rotation: Float,
        viewportWidth: Float
    ) {
        viewModelScope.launch {
            _cropState.value = AvatarCropUiState.Processing
            val success = withContext(Dispatchers.IO) {
                try {
                    val cropped = AvatarCropUtils.cropAvatar(originalBitmap, scale, offsetX, offsetY, rotation, viewportWidth)
                    val destFile = File(context.filesDir, "self_avatar.png")
                    val saved = AvatarCropUtils.saveAvatar(cropped, destFile)
                    if (saved) {
                        loadAvatar()
                    }
                    saved
                } catch (e: Exception) {
                    false
                }
            }
            if (success) {
                _cropState.value = AvatarCropUiState.Success
            } else {
                _cropState.value = AvatarCropUiState.Failure
            }
        }
    }
}

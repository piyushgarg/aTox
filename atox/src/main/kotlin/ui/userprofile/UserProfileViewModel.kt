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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview

import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.core.model.User
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.domain.feature.UserManager
import ltd.evilcorp.domain.tox.ITox
import ltd.evilcorp.domain.feature.FileTransferManager
import ltd.evilcorp.domain.usecase.SaveAvatarUseCase
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
    private val tox: ITox,
    private val fileTransferManager: FileTransferManager,
    private val saveAvatarUseCase: SaveAvatarUseCase,
) : ViewModel() {
    val publicKey by lazy { tox.publicKey }
    val toxId by lazy { tox.toxId }
    val user: StateFlow<User?> = userManager.get(publicKey)
        
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _avatar = MutableStateFlow<Bitmap?>(null)
    val avatar: StateFlow<Bitmap?> = _avatar.asStateFlow()

    private val nameUpdates = MutableSharedFlow<String>()
    private val statusUpdates = MutableSharedFlow<String>()

    init {
        loadAvatar()

        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            nameUpdates.debounce(800).collectLatest { name ->
                withContext(Dispatchers.IO) {
                    userManager.setName(name)
                }
            }
        }

        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            statusUpdates.debounce(800).collectLatest { status ->
                withContext(Dispatchers.IO) {
                    userManager.setStatusMessage(status)
                }
            }
        }
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
                    
                    // Compress bitmap to bytes to pass to use case
                    var quality = 90
                    var bytes: ByteArray? = null
                    val maxBytes = 64 * 1024
                    
                    try {
                        while (quality > 10) {
                            java.io.ByteArrayOutputStream().use { bos ->
                                cropped.compress(Bitmap.CompressFormat.JPEG, quality, bos)
                                val currentBytes = bos.toByteArray()
                                if (currentBytes.size <= maxBytes) {
                                    bytes = currentBytes
                                    break
                                }
                            }
                            quality -= 10
                        }
                        if (bytes == null) {
                            java.io.ByteArrayOutputStream().use { bos ->
                                cropped.compress(Bitmap.CompressFormat.JPEG, 10, bos)
                                val currentBytes = bos.toByteArray()
                                if (currentBytes.size <= maxBytes) {
                                    bytes = currentBytes
                                }
                            }
                        }
                    } finally {
                        cropped.recycle()
                    }
                    
                    val saveBytes = bytes ?: return@withContext false
                    val saved = saveAvatarUseCase.execute(saveBytes)
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

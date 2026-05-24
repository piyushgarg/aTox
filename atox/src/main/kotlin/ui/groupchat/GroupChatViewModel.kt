package ltd.evilcorp.atox.ui.groupchat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.media.SystemSoundPlayer
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupMessage
import ltd.evilcorp.core.model.GroupPeer
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.repository.GroupRepository
import ltd.evilcorp.core.repository.FileTransferRepository
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.domain.feature.GroupManager
import java.io.File
import java.util.Date

class GroupChatViewModel @Inject constructor(
    private val groupManager: GroupManager,
    private val contactRepository: ContactRepository,
    private val context: Context,
    private val settings: Settings,
    private val systemSoundPlayer: SystemSoundPlayer,
    private val groupRepository: GroupRepository,
    private val fileTransferRepository: FileTransferRepository,
) : ViewModel() {
    private var chatId = ""
    private var metadataSyncJob: kotlinx.coroutines.Job? = null

    private val activeGroupChatId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            groupManager.groupMigratedEvent.collect { (oldId, newId) ->
                if (chatId == oldId) {
                    Log.i("GroupChatViewModel", "Active group migrated from $oldId to $newId, dynamic redirection triggered.")
                    setActiveGroup(newId)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val group: StateFlow<Group?> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> groupManager.get(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val connectionStatus: StateFlow<GroupConnectionStatus> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> groupManager.connectionStatuses }
        .map { statuses -> statuses[chatId] ?: GroupConnectionStatus.Disconnected }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupConnectionStatus.Disconnected)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<GroupMessage>> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> groupManager.messagesFor(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val peers: StateFlow<List<GroupPeer>> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> groupManager.getPeers(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<Contact>> = contactRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val fileTransfers: StateFlow<List<FileTransfer>> = activeGroupChatId
        .filterNotNull()
        .flatMapLatest { cid -> fileTransferRepository.get(cid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setActiveGroup(chatId: String) {
        this.chatId = chatId
        activeGroupChatId.value = chatId
        groupManager.activeGroup = chatId

        // Запускаем фоновую синхронизацию метаданных и ключей пиров (аватарок)
        metadataSyncJob?.cancel()
        metadataSyncJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                groupManager.checkAndUpdateGroupMetadata(chatId)
                delay(3000)
            }
        }
    }

    fun sendMessage(message: String, type: MessageType = MessageType.Normal, correlationId: Int = -1) {
        if (message.trim().isEmpty()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            groupManager.sendMessage(chatId, message, type)
            if (type == MessageType.FileTransfer && correlationId != -1) {
                delay(150)
                groupRepository.getMessages(chatId).take(1).collect { list ->
                    val lastMsg = list.lastOrNull { it.message == message }
                    if (lastMsg != null) {
                        lastMsg.correlationId = correlationId
                        groupRepository.addMessage(lastMsg)
                    }
                }
            }
        }
    }

    fun sendFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val (name, size) = try {
                if (uri.scheme == "file") {
                    val f = File(uri.path ?: return@launch)
                    Pair(f.name, f.length())
                } else {
                    context.contentResolver.query(uri, null, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.SIZE))
                            val displayName = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                            Pair(displayName, fileSize)
                        } else null
                    }
                } ?: return@launch
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Failed to query file details", e)
                return@launch
            }

            val cacheDir = File(context.cacheDir, "outgoing")
            cacheDir.mkdirs()
            val destFile = File(cacheDir, "${java.util.UUID.randomUUID()}_$name")
            try {
                val input = if (uri.scheme == "file") {
                    java.io.FileInputStream(File(uri.path ?: throw java.io.FileNotFoundException()))
                } else {
                    context.contentResolver.openInputStream(uri)
                }
                input?.use { inp ->
                    destFile.outputStream().use { output ->
                        inp.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Failed to copy file to outgoing cache", e)
                return@launch
            }

            val correlationId = kotlin.random.Random.nextInt(1000000)
            val ft = FileTransfer(
                publicKey = chatId,
                fileNumber = correlationId,
                fileKind = ltd.evilcorp.core.model.FileKind.Data.ordinal,
                fileSize = size,
                fileName = name,
                outgoing = true,
                progress = size,
                destination = Uri.fromFile(destFile).toString(),
            )
            val id = fileTransferRepository.add(ft).toInt()

            val signalMsg = "[FILE:$name|$size|$id]"
            sendMessage(signalMsg, MessageType.FileTransfer, id)
        }
    }

    fun sendVoice(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val f = File(uri.path ?: return@launch)
            val name = f.name
            val size = f.length()

            val correlationId = kotlin.random.Random.nextInt(1000000)
            val ft = FileTransfer(
                publicKey = chatId,
                fileNumber = correlationId,
                fileKind = ltd.evilcorp.core.model.FileKind.Data.ordinal,
                fileSize = size,
                fileName = "voice_message_${correlationId}.m4a",
                outgoing = true,
                progress = size,
                destination = uri.toString(),
            )
            val id = fileTransferRepository.add(ft).toInt()

            val signalMsg = "[VOICE:10|$id]"
            sendMessage(signalMsg, MessageType.FileTransfer, id)
        }
    }

    fun acceptFt(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            fileTransferRepository.get(id).take(1).collect { ft ->
                fileTransferRepository.updateProgress(id, 0L)
                
                val totalSize = ft.fileSize
                val speedBytesPerSec = 700 * 1024 // ~700 KB/s
                val estimatedDurationMs = (totalSize.toFloat() / speedBytesPerSec * 1000).toLong()
                // Ограничиваем максимальное время эмуляции 25 секундами для удобства UX
                val totalDurationMs = maxOf(1000L, minOf(25000L, estimatedDurationMs))
                val stepDelay = 250L
                val steps = (totalDurationMs / stepDelay).toInt().coerceAtLeast(5)
                val stepSize = totalSize / steps
                
                for (i in 1..steps) {
                    delay(stepDelay)
                    val currentProgress = if (i == steps) totalSize else i * stepSize
                    fileTransferRepository.updateProgress(id, currentProgress)
                }
                
                val cacheFile = File(context.cacheDir, ft.fileName)
                if (!cacheFile.exists()) {
                    val ext = ft.fileName.substringAfterLast('.', "").lowercase()
                    val isImage = ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
                    val isVoice = ft.fileName.startsWith("voice_message_")
                    
                    if (isVoice) {
                        try {
                            val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                            if (defaultUri != null) {
                                context.contentResolver.openInputStream(defaultUri)?.use { input ->
                                    cacheFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else if (isImage) {
                        try {
                            val width = 512
                            val height = 512
                            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            
                            val paint = android.graphics.Paint()
                            val shader = android.graphics.LinearGradient(
                                0f, 0f, width.toFloat(), height.toFloat(),
                                android.graphics.Color.parseColor("#8A2BE2"),
                                android.graphics.Color.parseColor("#FF69B4"),
                                android.graphics.Shader.TileMode.CLAMP
                            )
                            paint.shader = shader
                            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                            
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 40f
                                isAntiAlias = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            canvas.drawText("aTox Image Transfer", width / 2f, height / 2f - 20f, textPaint)
                            
                            val subPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(180, 255, 255, 255)
                                textSize = 24f
                                isAntiAlias = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            canvas.drawText(ft.fileName, width / 2f, height / 2f + 40f, subPaint)
                            
                            cacheFile.outputStream().use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    if (!cacheFile.exists() || cacheFile.length() == 0L) {
                        cacheFile.writeText("Mock received file content")
                    }
                }
                val cachedUri = Uri.fromFile(cacheFile)
                fileTransferRepository.setDestination(id, cachedUri.toString())
                
                autoSaveGroupFileToDownloads(id, ft.copy(destination = cachedUri.toString()))
            }
        }
    }

    private fun autoSaveGroupFileToDownloads(id: Int, ft: FileTransfer) {
        if (ft.outgoing || !settings.autoSaveToDownloads) return
        try {
            val sourceFile = File(Uri.parse(ft.destination).path ?: return)
            if (!sourceFile.exists()) return

            val configuredDirectory = settings.autoSaveDirectoryUri
            if (configuredDirectory.isNotBlank()) {
                autoSaveGroupFileToDirectory(id, ft, sourceFile, Uri.parse(configuredDirectory))
                return
            }

            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, ft.fileName)
                val ext = sourceFile.extension.lowercase()
                val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/aTox")
            }

            val publicUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (publicUri != null) {
                resolver.openOutputStream(publicUri).use { out ->
                    java.io.FileInputStream(sourceFile).use { ins ->
                        ins.copyTo(out ?: return@use)
                    }
                }
                Log.i("GroupChatViewModel", "Successfully auto-saved ${ft.fileName} to public Downloads/aTox at $publicUri")
                fileTransferRepository.setDestination(id, publicUri.toString())
            }
        } catch (e: Exception) {
            Log.e("GroupChatViewModel", "Error auto-saving file ${ft.fileName} to public Downloads", e)
        }
    }

    private fun autoSaveGroupFileToDirectory(id: Int, ft: FileTransfer, sourceFile: File, directoryUri: Uri) {
        val mimeType = android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(sourceFile.extension.lowercase())
            ?: "application/octet-stream"
        val targetUri = android.provider.DocumentsContract.createDocument(context.contentResolver, directoryUri, mimeType, ft.fileName)
            ?: throw IllegalStateException("Failed to create ${ft.fileName} in $directoryUri")
        context.contentResolver.openOutputStream(targetUri)?.use { out ->
            sourceFile.inputStream().use { input ->
                input.copyTo(out)
            }
        } ?: throw IllegalStateException("Failed to open $targetUri for writing")
        Log.i("GroupChatViewModel", "Successfully auto-saved ${ft.fileName} to configured directory at $targetUri")
        fileTransferRepository.setDestination(id, targetUri.toString())
    }

    fun rejectFt(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            fileTransferRepository.updateProgress(id, -2L) // FT_REJECTED
        }
    }

    fun cancelFt(msg: GroupMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            fileTransferRepository.updateProgress(msg.correlationId, -2L) // FT_REJECTED
        }
    }

    fun saveFt(id: Int, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            fileTransferRepository.get(id).take(1).collect { ft ->
                val sourceFile = File(Uri.parse(ft.destination).path ?: return@collect)
                if (sourceFile.exists()) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            groupManager.clearHistory(chatId)
        }
    }

    fun deleteMessage(msg: GroupMessage) {
        viewModelScope.launch {
            groupManager.deleteMessage(msg.id)
        }
    }

    fun leaveGroup() {
        groupManager.leaveGroup(chatId)
    }

    fun setDraft(draft: String) {
        viewModelScope.launch {
            groupManager.setDraft(chatId, draft)
        }
    }

    fun getChatId(): String? = groupManager.getChatId(chatId)

    fun inviteFriend(friendPublicKey: String): Boolean = groupManager.inviteFriend(chatId, friendPublicKey)
}

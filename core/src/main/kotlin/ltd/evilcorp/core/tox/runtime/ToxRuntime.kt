package ltd.evilcorp.core.tox.runtime

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.forEach as kForEach
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ltd.evilcorp.core.model.FileKind
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.core.tox.ToxID
import ltd.evilcorp.core.tox.bootstrap.BootstrapNodeRegistry
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.save.SaveManager
import ltd.evilcorp.core.tox.save.SaveOptions

private const val TAG = "ToxRuntime"
private const val SLOW_ITERATION_LIMIT_MS = 10
private const val TOX_SALT_LENGTH = 32
private const val STOP_DELAY_MS = 10L
private const val BOOTSTRAP_NODES_COUNT = 4

/**
 * Главный исполнительный слой (Tox Runtime).
 * Управляет корутинными циклами фоновой обработки событий (`iterate()`), периодическим
 * автосохранением сессии, а также проксирует вызовы функций ядра Tox из прикладного уровня.
 */
@Singleton
class ToxRuntime @Inject constructor(
    private val scope: CoroutineScope,
    private val saveManager: SaveManager,
    private val nodeRegistry: BootstrapNodeRegistry,
) {
    /** Уникальный идентификатор Tox ID текущей сессии (76 символов). */
    val toxId: ToxID get() = toxWrapper.getToxId()

    /** Публичный ключ текущего пользователя (32 байта). */
    val publicKey: PublicKey get() = toxWrapper.getPublicKey()

    /**
     * Системное nospam-значение пользователя.
     * Используется для защиты от нежелательных запросов добавления в друзья.
     */
    var nospam: Int
        get() = toxWrapper.getNospam()
        set(value) = toxWrapper.setNospam(value)

    /** Указывает, запущена ли сессия Tox в данный момент. */
    var started = false
        private set

    /** Указывает, требуется ли выполнить повторное подключение к публичным DHT-узлам (bootstrap). */
    var isBootstrapNeeded = true

    /** Текущий пароль шифрования профиля (null, если профиль не зашифрован). */
    var password: String? = null
        private set

    private var running = false
    private var toxAvRunning = false
    private var passkey: ByteArray? = null
    private lateinit var toxWrapper: ToxWrapper

    private val saveMutex = Mutex()

    /**
     * Запускает нативное ядро Tox, расшифровывает профиль при наличии пароля и запускает асинхронные циклы итераций.
     * @param saveOption Параметры инициализации и бинарные данные профиля.
     * @param password Пароль шифрования профиля.
     * @param listener Слушатель базовых событий Tox (сообщения, статусы, файлы).
     * @param avListener Слушатель аудио- и видеозвонков.
     */
    fun start(saveOption: SaveOptions, password: String?, listener: ToxEventListener, avListener: ToxAvEventListener) {
        val nativeTox = NativeTox()
        toxWrapper = if (password == null) {
            passkey = null
            ToxWrapper(listener, avListener, saveOption)
        } else {
            val salt = nativeTox.getSalt(saveOption.saveData ?: ByteArray(0))
            if (salt != null) {
                passkey = nativeTox.passKeyDeriveWithSalt(password.toByteArray(), salt)
            }
            ToxWrapper(
                listener,
                avListener,
                saveOption.copy(
                    saveData = if (passkey != null) {
                        nativeTox.passDecrypt(saveOption.saveData ?: ByteArray(0), passkey!!)
                    } else {
                        saveOption.saveData
                    },
                ),
            )
        }

        this.password = password
        started = true
        save()
        iterateForever()
        iterateForeverAv()
    }

    /**
     * Останавливает сессию Tox, завершает циклы обработки, сохраняет профиль на диск и очищает ключи шифрования в памяти.
     * @return Объект корутины [Job].
     */
    fun stop(): Job = scope.launch {
        running = false
        while (started) {
            delay(STOP_DELAY_MS)
        }
        save().join()
        toxWrapper.stop()
        passkey = null
    }

    /**
     * Изменяет или снимает пароль шифрования текущего профиля, после чего инициирует немедленное сохранение.
     * @param new Новый пароль. Передайте null или пустую строку для удаления шифрования.
     */
    fun changePassword(new: String?) {
        passkey = if (new.isNullOrEmpty()) {
            null
        } else {
            val salt = ByteArray(TOX_SALT_LENGTH)
            Random.Default.nextBytes(salt)
            NativeTox().passKeyDeriveWithSalt(new.toByteArray(), salt)
        }
        password = new
        save()
    }

    /**
     * Возвращает список всех друзей, добавленных в список контактов ядра Tox.
     */
    fun getContacts(): List<Pair<PublicKey, Int>> = toxWrapper.getContacts()

    /**
     * Принимает запрос на добавление в друзья от другого пользователя.
     * @param publicKey Публичный ключ отправителя запроса.
     */
    fun acceptFriendRequest(publicKey: PublicKey) {
        toxWrapper.acceptFriendRequest(publicKey)
        save()
    }

    /**
     * Инициирует возобновление или принятие передачи файла.
     */
    fun startFileTransfer(pk: PublicKey, fileNumber: Int) {
        Log.i(TAG, "Starting file transfer $fileNumber from ${pk.fingerprint()}")
        toxWrapper.startFileTransfer(pk, fileNumber)
    }

    /**
     * Приостанавливает передачу файла.
     */
    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {
        Log.i(TAG, "Stopping file transfer $fileNumber from ${pk.fingerprint()}")
        toxWrapper.stopFileTransfer(pk, fileNumber)
    }

    /**
     * Отправляет запрос на передачу файла другу.
     */
    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String) =
        toxWrapper.sendFile(pk, fileKind, fileSize, fileName)

    /**
     * Отправляет очередной бинарный блок данных в процессе передачи файла.
     */
    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> =
        toxWrapper.sendFileChunk(pk, fileNo, pos, data)

    /**
     * Возвращает публичное имя текущего пользователя.
     */
    fun getName() = toxWrapper.getName()

    /**
     * Устанавливает новое имя пользователя и сохраняет профиль.
     */
    fun setName(name: String) {
        toxWrapper.setName(name)
        save()
    }

    /**
     * Возвращает текущее статусное сообщение пользователя.
     */
    fun getStatusMessage() = toxWrapper.getStatusMessage()

    /**
     * Устанавливает новое статусное сообщение и сохраняет профиль.
     */
    fun setStatusMessage(statusMessage: String) {
        toxWrapper.setStatusMessage(statusMessage)
        save()
    }

    /**
     * Отправляет запрос на добавление нового друга по его адресу Tox ID.
     */
    fun addContact(toxId: ToxID, message: String) {
        toxWrapper.addContact(toxId, message)
        save()
    }

    /**
     * Удаляет друга из списка контактов.
     */
    fun deleteContact(publicKey: PublicKey) {
        toxWrapper.deleteContact(publicKey)
        save()
    }

    /**
     * Отправляет текстовое сообщение другу.
     */
    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType) =
        toxWrapper.sendMessage(publicKey, message, type)

    /**
     * Возвращает полную копию бинарных данных профиля (зашифрованных, если пароль установлен).
     */
    fun getSaveData(): ByteArray {
        val currentPasskey = passkey
        val saveData = toxWrapper.getSaveData()
        return if (currentPasskey == null) {
            saveData
        } else {
            NativeTox().passEncrypt(saveData, currentPasskey) ?: saveData
        }
    }

    /**
     * Устанавливает или снимает статус набора текста (typing indicator) для друга.
     */
    fun setTyping(publicKey: PublicKey, typing: Boolean) =
        toxWrapper.setTyping(publicKey, typing)

    /**
     * Возвращает текущий сетевой статус присутствия (Online, Away, Busy).
     */
    fun getStatus() = toxWrapper.getStatus()

    /**
     * Устанавливает новый статус присутствия пользователя и сохраняет профиль.
     */
    fun setStatus(status: UserStatus) {
        toxWrapper.setStatus(status)
        save()
    }

    /**
     * Отправляет произвольный непотеряемый пакет (lossless custom packet) другу.
     */
    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray) =
        toxWrapper.sendLosslessPacket(pk, packet)

    /** Начинает аудио/видеовызов. */
    fun startCall(pk: PublicKey) = toxWrapper.startCall(pk)
    /** Принимает входящий вызов. */
    fun answerCall(pk: PublicKey) = toxWrapper.answerCall(pk)
    /** Завершает вызов. */
    fun endCall(pk: PublicKey) = toxWrapper.endCall(pk)

    /** Отправляет кадр звука PCM участнику вызова. */
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) =
        toxWrapper.sendAudio(pk, pcm, channels, samplingRate)

    /** Фоновый цикл обработки аудио- и видеозвонков. */
    private fun iterateForeverAv() = scope.launch {
        toxAvRunning = true
        while (running) {
            toxWrapper.iterateAv()
            delay(toxWrapper.iterationIntervalAv())
        }
        toxAvRunning = false
    }

    /** Главный фоновый цикл обработки событий P2P-сети Tox Core. */
    private fun iterateForever() = scope.launch {
        running = true
        while (running || toxAvRunning) {
            if (isBootstrapNeeded) {
                try {
                    bootstrap()
                    isBootstrapNeeded = false
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }

            val before = System.currentTimeMillis()
            toxWrapper.iterate()
            val timeTaken = System.currentTimeMillis() - before
            val iterationInterval = toxWrapper.iterationInterval()
            if (timeTaken > SLOW_ITERATION_LIMIT_MS && timeTaken > iterationInterval) {
                Log.w(TAG, "Tox thread overran: $timeTaken/$iterationInterval.")
            }
            delay(iterationInterval - timeTaken)
        }
        started = false
    }

    /** Выполняет подключение к предопределенному пулу публичных серверов DHT (Bootstrap). */
    private fun bootstrap() {
        nodeRegistry.get(BOOTSTRAP_NODES_COUNT).kForEach { node ->
            Log.i(TAG, "Bootstrapping from $node")
            toxWrapper.bootstrap(node.address, node.port, node.publicKey.bytes())
        }
    }

    /** Фоновое асинхронное автосохранение файла настроек на накопитель устройства. */
    private fun save(): Job = scope.launch {
        saveMutex.withLock {
            if (!started) {
                return@withLock
            }

            val saveData = toxWrapper.getSaveData()
            val encryptedData = if (passkey != null) {
                NativeTox().passEncrypt(saveData, passkey!!) ?: saveData
            } else {
                saveData
            }
            saveManager.save(publicKey, encryptedData)
        }
    }
}

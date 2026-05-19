package ltd.evilcorp.core.tox.save

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import java.io.File
import javax.inject.Inject
import ltd.evilcorp.core.model.PublicKey

private const val TAG = "AndroidSaveManager"

/**
 * Интерфейс менеджера сохранения профилей Tox.
 * Отвечает за сериализацию и десериализацию бинарных файлов состояний.
 */
interface SaveManager {
    /**
     * Возвращает список названий всех сохраненных профилей на диске.
     */
    fun list(): List<String>

    /**
     * Выполняет атомарную запись бинарного состояния профиля на диск.
     * @param pk Публичный ключ профиля, выступающий в качестве уникального идентификатора.
     * @param saveData Массив байтов состояния Tox Core.
     */
    fun save(pk: PublicKey, saveData: ByteArray)

    /**
     * Загружает бинарное состояние профиля с диска.
     * @param pk Публичный ключ профиля.
     * @return Массив байтов состояния Tox Core, либо null, если профиль не найден.
     */
    fun load(pk: PublicKey): ByteArray?

    /**
     * Удаляет файл сохранения профиля с диска.
     * @param pk Публичный ключ профиля.
     * @return true, если файл успешно удален, иначе false.
     */
    fun delete(pk: PublicKey): Boolean
}

/**
 * Android-реализация интерфейса [SaveManager], выполняющая атомарную запись файлов сохранения во внутреннюю директорию файлов приложения.
 */
class AndroidSaveManager @Inject constructor(val context: Context) : SaveManager {
    private val saveDir get() = context.filesDir

    override fun list(): List<String> =
        saveDir.listFiles()?.filter { it.extension == "tox" }?.map(File::nameWithoutExtension) ?: listOf()

    override fun save(pk: PublicKey, saveData: ByteArray) = AtomicFile(fileFor(pk)).run {
        Log.i(TAG, "Saving profile to $baseFile")
        val stream = startWrite()
        try {
            stream.write(saveData)
            finishWrite(stream)
        } catch (e: Exception) {
            failWrite(stream)
            throw e
        }
    }

    override fun load(pk: PublicKey): ByteArray? = fileFor(pk).let { saveFile ->
        if (saveFile.exists()) {
            saveFile.readBytes()
        } else {
            null
        }
    }

    override fun delete(pk: PublicKey): Boolean = fileFor(pk).delete()

    private fun fileFor(pk: PublicKey) = File("$saveDir/${pk.string()}.tox")
}

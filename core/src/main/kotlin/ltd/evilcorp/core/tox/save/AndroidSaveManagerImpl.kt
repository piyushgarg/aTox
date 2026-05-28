package ltd.evilcorp.core.tox.save

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import java.io.File
import javax.inject.Inject
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.save.ISaveManager

private const val TAG = "AndroidSaveManagerImpl"

/**
 * Android implementation of [ISaveManager] interface that performs atomic write of save files to the application internal files directory.
 */
class AndroidSaveManagerImpl @Inject constructor(val context: Context) : ISaveManager {
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

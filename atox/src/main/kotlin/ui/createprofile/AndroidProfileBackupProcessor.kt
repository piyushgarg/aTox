package ltd.evilcorp.atox.ui.createprofile

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidProfileBackupProcessor @Inject constructor(
    private val context: Context
) : ProfileBackupProcessor {
    override suspend fun readBackupBytes(uriString: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { it.readBytes() }
        }.getOrNull()
    }
}

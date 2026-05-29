package ltd.evilcorp.atox.infrastructure.sharing

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import ltd.evilcorp.atox.SharedContent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "IncomingShareProcessor"

@Singleton
class IncomingShareProcessor @Inject constructor(
    private val context: Context,
    private val sharedContentRegistry: SharedContentRegistry
) {
    fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> handleShareIntent(intent)
        }
    }

    private fun handleShareIntent(intent: Intent) {
        try {
            val action = intent.action
            val type = intent.type
            if (Intent.ACTION_SEND == action && type != null) {
                if (type.startsWith("text/")) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrEmpty()) {
                        sharedContentRegistry.setSharedContent(SharedContent.Text(sharedText))
                        Log.i(TAG, "Parsed shared text: $sharedText")
                    }
                } else {
                    val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    if (streamUri != null) {
                        sharedContentRegistry.setSharedContent(SharedContent.File(streamUri, type))
                        Log.i(TAG, "Parsed shared file URI: $streamUri")
                    }
                }
            } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
                val streamUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (streamUris != null) {
                    sharedContentRegistry.setSharedContent(
                        SharedContent.MultipleFiles(streamUris.filterNotNull())
                    )
                    Log.i(TAG, "Parsed shared multiple URIs: $streamUris")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle share intent", e)
        }
    }

    fun prepareShareUri(uri: Uri): Uri {
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(requireNotNull(uri.path))
            )
        }

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            val suffix = if (extension.isNotEmpty()) ".$extension" else ""
            val stagedFile = File(sharedDir, "shared_${System.currentTimeMillis()}$suffix")
            context.contentResolver.openInputStream(uri)?.use { input ->
                stagedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: error("Unable to open $uri for sharing")
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                stagedFile
            )
        }

        return uri
    }
}

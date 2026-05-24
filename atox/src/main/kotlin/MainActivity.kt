package ltd.evilcorp.atox

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import ltd.evilcorp.domain.feature.CallState
import ltd.evilcorp.domain.feature.GroupInvite
import ltd.evilcorp.domain.feature.GroupManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import ltd.evilcorp.atox.appearance.AppearanceManager
import ltd.evilcorp.atox.service.AutoAway
import ltd.evilcorp.atox.di.ViewModelFactory
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.media.SystemSoundPlayer
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.atox.util.PermissionManager
import ltd.evilcorp.atox.ui.navigation.AToxNavGraph
import ltd.evilcorp.atox.ui.theme.AToxTheme
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.FINGERPRINT_LEN
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.core.tox.TOX_ID_LENGTH
import java.io.File

private const val TAG = "MainActivity"
private const val SCHEME = "tox:"

const val CONTACT_PUBLIC_KEY = "contact_public_key"
const val FOCUS_ON_MESSAGE_BOX = "focus_on_message_box"

sealed class SharedContent : java.io.Serializable {
    data class Text(val text: String) : SharedContent()
    data class File(val uri: Uri, val mimeType: String?) : SharedContent()
    data class MultipleFiles(val uris: List<Uri>) : SharedContent()
}

class MainActivity : AppCompatActivity() {
    companion object {
        val sharedContentState = mutableStateOf<SharedContent?>(null)
    }
    @Inject
    lateinit var vmFactory: ViewModelFactory

    @Inject
    lateinit var autoAway: AutoAway

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var callManager: CallManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var appearanceManager: AppearanceManager

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var systemSoundPlayer: SystemSoundPlayer

    @Inject
    lateinit var groupManager: GroupManager

    private var groupInviteDialog: android.app.AlertDialog? = null
    private val initialToxIdToLink = mutableStateOf<String?>(null)
    private val callScreenMinimized = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as App).component.inject(this)

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        updateSecureWindow(settings.disableScreenshots)

        setContent {
            val appearance by appearanceManager.appearance.collectAsState()
            val isDarkTheme = when (appearance.themeMode) {
                AppCompatDelegate.MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            }

            LaunchedEffect(appearance, isDarkTheme) {
                syncSystemBars(isDarkTheme)
            }

            AToxTheme(
                darkTheme = isDarkTheme,
                dynamicColor = appearance.dynamicColorEnabled,
                accentColorSeedArgb = appearance.accentColorSeed
            ) {
                AToxNavGraph(
                    appearance = appearance,
                    settings = settings,
                    vmFactory = vmFactory,
                    callManager = callManager,
                    notificationHelper = notificationHelper,
                    permissionManager = permissionManager,
                    systemSoundPlayer = systemSoundPlayer,
                    initialToxIdToLink = initialToxIdToLink,
                    callScreenMinimized = callScreenMinimized,
                    onOpenFile = ::openFile,
                    onQuitApp = ::finish,
                    onThemeChanged = appearanceManager::updateThemeMode,
                    onDynamicColorChanged = appearanceManager::updateDynamicColorEnabled,
                    onAccentColorSeedChanged = appearanceManager::updateAccentColorSeed,
                    onLocaleTagChanged = ::updateLocale,
                    onDisableScreenshotsChanged = { disable ->
                        settings.disableScreenshots = disable
                        updateSecureWindow(disable)
                    },
                )
            }
        }

        lifecycleScope.launch {
            groupManager.pendingInvite.collect { invite ->
                if (invite == null) {
                    groupInviteDialog?.dismiss()
                    groupInviteDialog = null
                    return@collect
                }

                if (groupInviteDialog != null) return@collect

                groupInviteDialog = android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.group_invite)
                    .setMessage(getString(R.string.group_invite_confirm, invite.groupName))
                    .setPositiveButton(R.string.confirm) { _, _ ->
                        groupManager.acceptInvite()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        groupManager.declineInvite()
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        if (savedInstanceState == null) {
            handleIntent(intent)
        }
    }

    private fun syncSystemBars(isDarkTheme: Boolean) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.apply {
            dimAmount = 0f
        }
        window.decorView.alpha = 1f
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    private fun updateSecureWindow(disableScreenshots: Boolean) {
        if (disableScreenshots) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun updateLocale(localeTag: String) {
        if (appearanceManager.appearance.value.localeTag == localeTag) return
        appearanceManager.updateLocaleTag(localeTag)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        autoAway.onBackground()
    }

    override fun onResume() {
        super.onResume()
        autoAway.onForeground()
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> handleToxLinkIntent(intent)
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> handleShareIntent(intent)
        }
    }

    private fun handleToxLinkIntent(intent: Intent) {
        val data = intent.dataString ?: ""
        Log.i(TAG, "Got uri with data: $data")
        if (!data.startsWith(SCHEME) || data.length != SCHEME.length + TOX_ID_LENGTH) {
            Log.e(TAG, "Got malformed uri: $data")
            return
        }

        val toxId = data.drop(SCHEME.length)
        initialToxIdToLink.value = toxId
    }

    private fun handleShareIntent(intent: Intent) {
        try {
            val action = intent.action
            val type = intent.type
            if (Intent.ACTION_SEND == action && type != null) {
                if (type.startsWith("text/")) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrEmpty()) {
                        sharedContentState.value = SharedContent.Text(sharedText)
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
                        sharedContentState.value = SharedContent.File(streamUri, type)
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
                    sharedContentState.value = SharedContent.MultipleFiles(streamUris.filterNotNull())
                    Log.i(TAG, "Parsed shared multiple URIs: $streamUris")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle share intent", e)
        }
    }

    private fun openFile(ft: FileTransfer) {
        lifecycleScope.launch {
            try {
                val uri = ft.destination.toUri()
                val shareUri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    prepareShareUri(uri)
                }

                val mimeType = contentResolver.getType(shareUri) ?: android.webkit.MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(File(ft.fileName).extension.lowercase()) ?: "*/*"

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(shareUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(intent)
                } catch (e: android.content.ActivityNotFoundException) {
                    startActivity(Intent.createChooser(intent, getString(R.string.open_with)))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open file ${ft.fileName}", e)
                val messageRes = if (e is SecurityException) {
                    R.string.open_file_security_failure
                } else {
                    R.string.open_file_failure
                }
                android.widget.Toast.makeText(this@MainActivity, messageRes, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prepareShareUri(uri: Uri): Uri {
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            return androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                File(requireNotNull(uri.path))
            )
        }

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val sharedDir = File(cacheDir, "shared").apply { mkdirs() }
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            val suffix = if (extension.isNotEmpty()) ".$extension" else ""
            val stagedFile = File(sharedDir, "shared_${System.currentTimeMillis()}$suffix")
            contentResolver.openInputStream(uri)?.use { input ->
                stagedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Unable to open $uri for sharing")
            return androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                stagedFile
            )
        }

        return uri
    }
}

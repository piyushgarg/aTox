package ltd.evilcorp.atox

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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.view.WindowCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import javax.inject.Inject
import ltd.evilcorp.atox.appearance.AppearanceManager
import ltd.evilcorp.atox.infrastructure.service.AutoAway
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.atox.infrastructure.util.PermissionManager
import ltd.evilcorp.atox.ui.navigation.AToxNavGraph
import ltd.evilcorp.atox.ui.navigation.ToxLinkManager
import ltd.evilcorp.atox.ui.theme.AToxTheme
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.call.CallManager
import ltd.evilcorp.domain.core.network.TOX_ID_LENGTH
import java.io.File

import ltd.evilcorp.atox.infrastructure.sharing.SharedContentRegistry
import androidx.compose.runtime.CompositionLocalProvider
import ltd.evilcorp.domain.core.network.IFileStorageProvider
import ltd.evilcorp.atox.ui.common.LocalFileStorageProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback
import ltd.evilcorp.atox.ui.common.LocalHapticEnabled
import ltd.evilcorp.atox.ui.common.AtoxHapticFeedback

private const val TAG = "MainActivity"
private const val SCHEME = "tox:"

const val CONTACT_PUBLIC_KEY = "contact_public_key"
const val FOCUS_ON_MESSAGE_BOX = "focus_on_message_box"

sealed class SharedContent : java.io.Serializable {
    data class Text(val text: String) : SharedContent()
    data class File(val uri: Uri, val mimeType: String?) : SharedContent()
    data class MultipleFiles(val uris: List<Uri>) : SharedContent()
}

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        // Removed static sharedContentState
    }


    @Inject
    lateinit var autoAway: dagger.Lazy<AutoAway>

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var callManager: dagger.Lazy<CallManager>

    @Inject
    lateinit var notificationHelper: dagger.Lazy<NotificationHelper>

    @Inject
    lateinit var appearanceManager: AppearanceManager

    @Inject
    lateinit var permissionManager: dagger.Lazy<PermissionManager>

    @Inject
    lateinit var systemSoundPlayer: dagger.Lazy<SystemSoundPlayer>

    @Inject
    lateinit var sharedContentRegistry: dagger.Lazy<SharedContentRegistry>

    @Inject
    lateinit var toxLinkManager: dagger.Lazy<ToxLinkManager>

    @Inject
    lateinit var fileStorageProvider: IFileStorageProvider

    @Inject
    @ltd.evilcorp.domain.core.di.IoDispatcher
    lateinit var ioDispatcher: kotlinx.coroutines.CoroutineDispatcher

    @Inject
    lateinit var incomingShareProcessor: ltd.evilcorp.atox.infrastructure.sharing.IncomingShareProcessor

    @Inject
    lateinit var localeInitializer: ltd.evilcorp.atox.appearance.LocaleInitializer

    @Inject
    lateinit var backupWorkScheduler: ltd.evilcorp.atox.infrastructure.backup.BackupWorkScheduler

    @Inject
    lateinit var userSettingsRepository: ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository

    private var callScreenMinimized = mutableStateOf(false)

    @Suppress("LongMethod")
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var isAppearanceLoaded = false
        val content: android.view.View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (isAppearanceLoaded) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }
            }
        )

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.CREATED) {
                launch {
                    appearanceManager.appearance.collect { appearance ->
                        localeInitializer.updateLocale(appearance.localeTag)
                        isAppearanceLoaded = true
                    }
                }
                launch {
                    userSettingsRepository.settings.collect { userSettings ->
                        backupWorkScheduler.scheduleBackups(userSettings)
                    }
                }
            }
        }

        enableEdgeToEdge()
        updateSecureWindow(settings.disableScreenshots)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val appearance by appearanceManager.appearance.collectAsState()
            val isDarkTheme = when (appearance.themeMode) {
                AppCompatDelegate.MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            }

            val userSettings by settings.state.collectAsState()
            val hapticEnabled = userSettings.hapticEnabled
            val systemHaptic = LocalHapticFeedback.current
            val customHaptic = remember(systemHaptic, hapticEnabled) {
                AtoxHapticFeedback(systemHaptic) { hapticEnabled }
            }

            CompositionLocalProvider(
                LocalFileStorageProvider provides fileStorageProvider,
                LocalHapticEnabled provides hapticEnabled,
                LocalHapticFeedback provides customHaptic
            ) {
                AToxTheme(
                    darkTheme = isDarkTheme,
                    dynamicColor = appearance.dynamicColorEnabled,
                    accentColorSeedArgb = appearance.accentColorSeed
                ) {
                    val surfaceContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
                    LaunchedEffect(isDarkTheme, surfaceContainerColor) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            window.isNavigationBarContrastEnforced = false
                        }
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

                    AToxNavGraph(
                        appearance = appearance,
                        settings = settings,
                        windowSizeClass = windowSizeClass,

                        callManager = callManager.get(),
                        notificationHelper = notificationHelper.get(),
                        permissionManager = permissionManager.get(),
                        systemSoundPlayer = systemSoundPlayer.get(),
                        toxLinkManager = toxLinkManager.get(),
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
        }



        if (savedInstanceState == null) {
            handleIntent(intent)
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
        autoAway.get().onBackground()
    }

    override fun onResume() {
        super.onResume()
        autoAway.get().onForeground()
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> handleToxLinkIntent(intent)
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> incomingShareProcessor.handleIntent(intent)
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
        toxLinkManager.get().setPendingToxId(toxId)
    }

    private fun openFile(ft: FileTransfer) {
        lifecycleScope.launch {
            try {
                val uri = ft.destination.toUri()
                val shareUri = kotlinx.coroutines.withContext(ioDispatcher) {
                    incomingShareProcessor.prepareShareUri(uri)
                }

                val mimeType = contentResolver.getType(shareUri) ?: MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(File(ft.fileName).extension.lowercase()) ?: "*/*"

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(shareUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(intent)
                } catch (_: android.content.ActivityNotFoundException) {
                    startActivity(Intent.createChooser(intent, getString(R.string.open_with)))
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security failure while opening file ${ft.fileName}", e)
                Toast.makeText(this@MainActivity, R.string.open_file_security_failure, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open file ${ft.fileName}", e)
                Toast.makeText(this@MainActivity, R.string.open_file_failure, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

package ltd.evilcorp.atox

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ltd.evilcorp.domain.feature.CallState
import androidx.navigation.navArgument
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
import ltd.evilcorp.atox.ui.addcontact.AddContactScreen
import ltd.evilcorp.atox.ui.addcontact.AddContactViewModel
import ltd.evilcorp.atox.ui.call.CallScreen
import ltd.evilcorp.atox.ui.call.CallViewModel
import ltd.evilcorp.atox.ui.chat.ChatScreen
import ltd.evilcorp.atox.ui.chat.ChatViewModel
import ltd.evilcorp.atox.ui.contactlist.ContactListScreen
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.createprofile.CreateProfileScreen
import ltd.evilcorp.atox.ui.createprofile.CreateProfileViewModel
import ltd.evilcorp.atox.ui.settings.SettingsScreen
import ltd.evilcorp.atox.ui.theme.AToxTheme
import ltd.evilcorp.atox.ui.userprofile.UserProfileScreen
import ltd.evilcorp.atox.ui.userprofile.UserProfileViewModel
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.FINGERPRINT_LEN
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.core.tox.TOX_ID_LENGTH
import ltd.evilcorp.core.tox.ToxID
import ltd.evilcorp.core.tox.save.ToxSaveStatus
import java.io.File

private const val TAG = "MainActivity"
private const val SCHEME = "tox:"

const val CONTACT_PUBLIC_KEY = "contact_public_key"
const val FOCUS_ON_MESSAGE_BOX = "focus_on_message_box"

class MainActivity : AppCompatActivity() {
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

    private var incomingCallDialog: android.app.AlertDialog? = null
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
                AppNavigation(appearance)
            }
        }

        lifecycleScope.launch {
            callManager.inCall.collect { state ->
                val incoming = state as? CallState.IncomingRinging
                if (incoming == null) {
                    incomingCallDialog?.dismiss()
                    incomingCallDialog = null
                    return@collect
                }

                if (incomingCallDialog != null) return@collect

                val contact = incoming.contact
                incomingCallDialog = android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.incoming_call)
                    .setMessage(getString(R.string.incoming_call_from, contact.name.ifEmpty { contact.publicKey.take(FINGERPRINT_LEN) }))
                    .setPositiveButton(R.string.accept) { _, _ ->
                        lifecycleScope.launch {
                            val pk = PublicKey(contact.publicKey)
                            if (callManager.acceptIncomingCall(pk)) {
                                notificationHelper.showOngoingCallNotification(contact)
                                notificationHelper.dismissCallNotification(pk)
                                callManager.startSendingAudio()
                            }
                        }
                    }
                    .setNegativeButton(R.string.reject) { _, _ ->
                        callManager.endCall(PublicKey(contact.publicKey))
                        notificationHelper.dismissCallNotification(PublicKey(contact.publicKey))
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

    @Composable
    private fun AppNavigation(appearance: ltd.evilcorp.atox.appearance.AppAppearance) {
        val navController = rememberNavController()

        val callState by callManager.inCall.collectAsState()
        Box(modifier = Modifier.fillMaxSize()) {
            LaunchedEffect(callState, callScreenMinimized.value) {
                val state = callState
                val publicKey = when (state) {
                    is CallState.OutgoingRequesting -> state.publicKey.string()
                    is CallState.OutgoingWaiting -> state.publicKey.string()
                    is CallState.Connecting -> state.publicKey.string()
                    is CallState.OutgoingRinging -> state.publicKey.string()
                    is CallState.Active -> state.publicKey.string()
                    else -> null
                }
                if (publicKey != null && !callScreenMinimized.value) {
                    navController.navigate("call/$publicKey") {
                        launchSingleTop = true
                    }
                } else {
                    if (navController.currentBackStackEntry?.destination?.route?.startsWith("call/") == true) {
                        navController.popBackStack()
                    }
                }
            }

            // Handle link intents
            LaunchedEffect(initialToxIdToLink.value) {
                initialToxIdToLink.value?.let { toxId ->
                    navController.navigate("add_contact?toxId=$toxId")
                    initialToxIdToLink.value = null
                }
            }

            NavHost(
                navController = navController,
                startDestination = "launch"
            ) {
                composable("launch") {
                    val viewModel: ContactListViewModel = viewModel(factory = vmFactory)
                    LaunchScreen(viewModel = viewModel, navController = navController)
                }

                composable("unlock") {
                    val viewModel: ContactListViewModel = viewModel(factory = vmFactory)
                    UnlockScreen(
                        viewModel = viewModel,
                        onUnlockSuccess = {
                            navController.navigate("contact_list") {
                                popUpTo("unlock") { inclusive = true }
                            }
                        },
                        onQuit = {
                            finish()
                        }
                    )
                }
                composable(
                    route = "contact_list",
                    exitTransition = {
                        if (targetState.destination.route?.startsWith("chat/") == true ||
                            targetState.destination.route?.startsWith("add_contact") == true) {
                            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300))
                        } else {
                            slideOutHorizontally(targetOffsetX = { 0 }, animationSpec = tween(0))
                        }
                    },
                    popEnterTransition = {
                        if (initialState.destination.route?.startsWith("chat/") == true ||
                            initialState.destination.route?.startsWith("add_contact") == true) {
                            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300))
                        } else {
                            slideInHorizontally(initialOffsetX = { 0 }, animationSpec = tween(0))
                        }
                    }
                ) {
                    val viewModel: ContactListViewModel = viewModel(factory = vmFactory)
                    val profileViewModel: UserProfileViewModel = viewModel(factory = vmFactory)
                    val addContactViewModel: AddContactViewModel = viewModel(factory = vmFactory)

                    val userState = viewModel.user.observeAsState()
                    val contactsState = viewModel.contacts.observeAsState(emptyList())
                    val friendRequestsState = viewModel.friendRequests.observeAsState(emptyList())

                    ContactListScreen(
                        userState = userState,
                        contactsState = contactsState,
                        friendRequestsState = friendRequestsState,
                        onAddContact = { toxIdStr, message -> addContactViewModel.addContact(ToxID(toxIdStr), message) },
                        onContactClick = { contact -> navController.navigate("chat/${contact.publicKey}") },
                        onDeleteContact = { contact -> viewModel.deleteContact(PublicKey(contact.publicKey)) },
                        onAcceptFriendRequest = { req -> viewModel.acceptFriendRequest(req) },
                        onRejectFriendRequest = { req -> viewModel.rejectFriendRequest(req) },
                        onQuitTox = {
                            if (viewModel.quittingNeedsConfirmation()) {
                                android.app.AlertDialog.Builder(this@MainActivity)
                                    .setTitle(R.string.quit)
                                    .setMessage(R.string.quit_confirm)
                                    .setPositiveButton(R.string.confirm) { _, _ ->
                                        viewModel.quitTox()
                                        finish()
                                    }
                                    .setNegativeButton(R.string.reject, null)
                                    .show()
                            } else {
                                viewModel.quitTox()
                                finish()
                            }
                        },
                        toxId = profileViewModel.toxId.string(),
                        onSetName = { name -> profileViewModel.setName(name) },
                        onSetStatusMessage = { status -> profileViewModel.setStatusMessage(status) },
                        onSetStatus = { status -> profileViewModel.setStatus(status) },
                        settings = settings,
                        appearance = appearance,
                        onThemeChanged = appearanceManager::updateThemeMode,
                        onDynamicColorChanged = appearanceManager::updateDynamicColorEnabled,
                        onAccentColorSeedChanged = appearanceManager::updateAccentColorSeed,
                        onLocaleTagChanged = ::updateLocale,
                        onDisableScreenshotsChanged = { disable ->
                            settings.disableScreenshots = disable
                            updateSecureWindow(disable)
                        },
                        onLogout = {
                            viewModel.deleteProfileAndData()
                            navController.navigate("launch") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onAvatarChanged = { profileViewModel.broadcastAvatar() },
                        vmFactory = vmFactory
                    )
                }

                composable("create_profile") {
                    val viewModel: CreateProfileViewModel = viewModel(factory = vmFactory)
                    CreateProfileScreen(
                        onCreateProfile = { name ->
                            val status = viewModel.createProfile(name)
                            if (status != ToxSaveStatus.Ok) {
                                return@CreateProfileScreen status
                            }
                            navController.navigate("contact_list") {
                                popUpTo("create_profile") { inclusive = true }
                            }
                            ToxSaveStatus.Ok
                        }
                    )
                }

                composable(
                    route = "chat/{publicKey}",
                    arguments = listOf(navArgument("publicKey") { type = NavType.StringType }),
                    enterTransition = {
                        slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300))
                    },
                    popExitTransition = {
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
                    }
                ) { backStackEntry ->
                    val publicKeyStr = backStackEntry.arguments?.getString("publicKey") ?: ""
                    val viewModel: ChatViewModel = viewModel(factory = vmFactory)

                    LaunchedEffect(publicKeyStr) {
                        viewModel.setActiveChat(PublicKey(publicKeyStr), deferSideEffects = true)
                    }

                    DisposableEffect(publicKeyStr) {
                        onDispose {
                            viewModel.clearActiveChatAfterNavigation(PublicKey(publicKeyStr))
                        }
                    }

                    val contactState = viewModel.contact.observeAsState()
                    val messagesState = viewModel.messages.observeAsState()
                    val fileTransfersState = viewModel.fileTransfers.observeAsState(emptyList())

                    ChatScreen(
                        contactState = contactState,
                        messagesState = messagesState,
                        fileTransfersState = fileTransfersState,
                        settings = settings,
                        onBack = {
                            navController.popBackStack()
                        },
                        onSendMessage = { content ->
                            viewModel.send(content, MessageType.Normal)
                        },
                        onTypingChanged = { typing ->
                            viewModel.setTyping(typing)
                        },
                        onSendFile = { uri ->
                            viewModel.createFt(uri)
                        },
                        onCallClick = {
                            viewModel.startCall()
                        },
                        onCallHistoryClick = {
                            viewModel.startCall()
                        },
                        onAcceptFt = { id ->
                            viewModel.acceptFt(id)
                        },
                        onRejectFt = { id ->
                            viewModel.rejectFt(id)
                        },
                        onCancelFt = { msg ->
                            viewModel.delete(msg)
                        },
                        onSaveFt = { id, destUri ->
                            viewModel.exportFt(id, destUri)
                        },
                        onOpenFile = { ft ->
                            openFile(ft)
                        },
                        systemSoundPlayer = systemSoundPlayer
                    )
                }

                composable(
                    route = "call/{publicKey}",
                    arguments = listOf(navArgument("publicKey") { type = NavType.StringType })
                ) { backStackEntry ->
                    val publicKeyStr = backStackEntry.arguments?.getString("publicKey") ?: ""
                    val viewModel: CallViewModel = viewModel(factory = vmFactory)

                    LaunchedEffect(publicKeyStr) {
                        viewModel.setActiveContact(PublicKey(publicKeyStr))
                        callScreenMinimized.value = false
                    }

                    val contactState = viewModel.contact.observeAsState()
                    val callStateState = viewModel.inCall.collectAsState()
                    val sendingAudioState = viewModel.sendingAudio.collectAsState()
                    val speakerphoneOnState = viewModel.speakerphoneState.collectAsState()

                    CallScreen(
                        contactState = contactState,
                        callState = callStateState,
                        sendingAudioState = sendingAudioState,
                        speakerphoneOnState = speakerphoneOnState,
                        connectedAtState = viewModel.connectedAt.collectAsState(initial = -1L),
                        permissionManager = permissionManager,
                        onMinimize = {
                            callScreenMinimized.value = true
                            navController.popBackStack()
                        },
                        onToggleMic = {
                            if (sendingAudioState.value) {
                                viewModel.stopSendingAudio()
                            } else {
                                viewModel.startSendingAudio()
                            }
                        },
                        onToggleSpeaker = {
                            viewModel.toggleSpeakerphone()
                        },
                        onEndCall = {
                            callScreenMinimized.value = false
                            viewModel.endCall()
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = "add_contact?toxId={toxId}",
                    arguments = listOf(navArgument("toxId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }),
                    enterTransition = {
                        slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300))
                    },
                    popExitTransition = {
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
                    }
                ) { backStackEntry ->
                    val toxIdArg = backStackEntry.arguments?.getString("toxId") ?: ""
                    val viewModel: AddContactViewModel = viewModel(factory = vmFactory)
                    AddContactScreen(
                        initialToxId = toxIdArg,
                        onBack = { navController.popBackStack() },
                        onAddContact = { toxIdStr, message ->
                            viewModel.addContact(ToxID(toxIdStr), message)
                            navController.popBackStack()
                        }
                    )
                }
            }

            val activeCallState = callState
            val publicKey = when (activeCallState) {
                is CallState.OutgoingRequesting -> activeCallState.publicKey.string()
                is CallState.OutgoingWaiting -> activeCallState.publicKey.string()
                is CallState.OutgoingRinging -> activeCallState.publicKey.string()
                is CallState.Connecting -> activeCallState.publicKey.string()
                is CallState.Active -> activeCallState.publicKey.string()
                else -> null
            }
            if (callScreenMinimized.value && publicKey != null) {
                ReturnToCallBanner(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                    onClick = {
                        callScreenMinimized.value = false
                    },
                )
            }
        }
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
            Intent.ACTION_SEND -> handleShareIntent(intent)
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
        if (intent.type != "text/plain") {
            Log.e(TAG, "Got unsupported share type ${intent.type}")
            return
        }

        val data = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (data.isNullOrEmpty()) {
            Log.e(TAG, "Got share intent with no data")
            return
        }

        Log.i(TAG, "Got text share: $data")
    }

    fun openFile(ft: FileTransfer) {
        try {
            val uri = ft.destination.toUri()
            val shareUri = prepareShareUri(uri)

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
            android.widget.Toast.makeText(this, messageRes, android.widget.Toast.LENGTH_SHORT).show()
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

    @Composable
    private fun ReturnToCallBanner(
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
    ) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            tonalElevation = 3.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = getString(R.string.return_to_call).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
fun LaunchScreen(
    viewModel: ContactListViewModel,
    navController: androidx.navigation.NavController
) {
    LaunchedEffect(Unit) {
        val status = viewModel.tryLoadTox(null)
        when (status) {
            ToxSaveStatus.Ok -> {
                navController.navigate("contact_list") {
                    popUpTo("launch") { inclusive = true }
                }
            }
            ToxSaveStatus.SaveNotFound -> {
                navController.navigate("create_profile") {
                    popUpTo("launch") { inclusive = true }
                }
            }
            ToxSaveStatus.Encrypted -> {
                navController.navigate("unlock") {
                    popUpTo("launch") { inclusive = true }
                }
            }
            else -> {
                navController.navigate("create_profile") {
                    popUpTo("launch") { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "aTox",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.secure_connection_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UnlockScreen(
    viewModel: ContactListViewModel,
    onUnlockSuccess: () -> Unit,
    onQuit: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val submitUnlock = {
        if (password.isNotEmpty() && !isLoading) {
            isLoading = true
            val status = viewModel.tryLoadTox(password)
            if (status == ToxSaveStatus.Ok) {
                onUnlockSuccess()
            } else {
                isError = true
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.unlock_profile_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.unlock_profile_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    isError = false
                },
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) stringResource(R.string.hide) else stringResource(R.string.show), fontSize = 14.sp)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submitUnlock() }
                ),
                isError = isError,
                supportingText = {
                    if (isError) {
                        Text(
                            text = stringResource(R.string.invalid_password),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { submitUnlock() },
                modifier = Modifier.fillMaxWidth(),
                enabled = password.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.unlock))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onQuit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.exit))
                    }
        }
    }
}

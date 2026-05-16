// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import ltd.evilcorp.atox.di.ViewModelFactory
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.atox.ui.chat.CONTACT_PUBLIC_KEY
import ltd.evilcorp.atox.ui.contactlist.ARG_ADD_CONTACT
import ltd.evilcorp.atox.ui.contactlist.ARG_SHARE
import ltd.evilcorp.core.vo.PublicKey
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.tox.FINGERPRINT_LEN

private const val TAG = "MainActivity"
private const val SCHEME = "tox:"
private const val TOX_ID_LENGTH = 76

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

    private var incomingCallDialog: android.app.AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as App).component.inject(this)

        super.onCreate(savedInstanceState)

        if (settings.disableScreenshots) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        AppCompatDelegate.setDefaultNightMode(settings.theme)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            callManager.pendingCalls.collect { calls ->
                if (calls.isEmpty()) {
                    incomingCallDialog?.dismiss()
                    incomingCallDialog = null
                    return@collect
                }

                if (incomingCallDialog != null) return@collect

                val contact = calls.first()
                incomingCallDialog = android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.incoming_call)
                    .setMessage(getString(R.string.incoming_call_from, contact.name.ifEmpty { contact.publicKey.take(FINGERPRINT_LEN) }))
                    .setPositiveButton(R.string.accept) { _, _ ->
                        val pk = PublicKey(contact.publicKey)
                        callManager.startCall(pk)
                        notificationHelper.showOngoingCallNotification(contact)
                        notificationHelper.dismissCallNotification(pk)
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.findNavController()?.navigate(
                            R.id.callFragment,
                            bundleOf(CONTACT_PUBLIC_KEY to contact.publicKey),
                        )
                    }
                    .setNegativeButton(R.string.reject) { _, _ ->
                        callManager.endCall(PublicKey(contact.publicKey))
                        notificationHelper.dismissCallNotification(PublicKey(contact.publicKey))
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        // Only handle intent the first time it triggers the app.
        if (savedInstanceState != null) return
        handleIntent(intent)
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

        supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.findNavController()?.navigate(
            R.id.contactListFragment,
            bundleOf(ARG_ADD_CONTACT to data.drop(SCHEME.length)),
        )
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
        val navController =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.findNavController() ?: return
        navController.navigate(R.id.contactListFragment, bundleOf(ARG_SHARE to data))
    }
}

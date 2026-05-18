// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.contactlist

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.tox.ToxStarter
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.User
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.feature.ChatManager
import ltd.evilcorp.domain.feature.ContactManager
import ltd.evilcorp.domain.feature.FileTransferManager
import ltd.evilcorp.domain.feature.FriendRequestManager
import ltd.evilcorp.domain.feature.UserManager
import ltd.evilcorp.core.tox.save.ProxyType
import ltd.evilcorp.core.tox.save.SaveOptions
import ltd.evilcorp.core.tox.save.testToxSave
import ltd.evilcorp.domain.tox.Tox
import ltd.evilcorp.core.tox.save.ToxSaveStatus
import ltd.evilcorp.core.tox.save.SaveManager
import ltd.evilcorp.core.db.Database

class ContactListViewModel @Inject constructor(
    private val scope: CoroutineScope,
    private val context: Context,
    private val resolver: ContentResolver,
    private val callManager: CallManager,
    private val chatManager: ChatManager,
    private val contactManager: ContactManager,
    private val fileTransferManager: FileTransferManager,
    private val friendRequestManager: FriendRequestManager,
    private val notificationHelper: NotificationHelper,
    private val tox: Tox,
    private val toxStarter: ToxStarter,
    private val settings: Settings,
    private val saveManager: SaveManager,
    private val database: Database,
    userManager: UserManager,
) : ViewModel() {
    val publicKey by lazy { tox.publicKey }

    val user: LiveData<User?> by lazy { userManager.get(publicKey).asLiveData() }
    val contacts: LiveData<List<Contact>> = contactManager.getAll().asLiveData()
    val friendRequests: LiveData<List<FriendRequest>> = friendRequestManager.getAll().asLiveData()

    fun isToxRunning() = tox.started
    fun tryLoadTox(password: String?): ToxSaveStatus = toxStarter.tryLoadTox(password)
    fun quitTox() = toxStarter.stopTox()

    fun deleteProfileAndData() {
        val pk = tox.publicKey
        toxStarter.stopTox()
        saveManager.delete(pk)
        saveManager.list().forEach {
            try {
                saveManager.delete(PublicKey(it))
            } catch (e: Exception) {
                // Ignore
            }
        }
        scope.launch(Dispatchers.IO) {
            database.clearAllTables()
        }
    }

    fun quittingNeedsConfirmation(): Boolean = settings.confirmQuitting

    fun acceptFriendRequest(friendRequest: FriendRequest) = friendRequestManager.accept(friendRequest)
    fun rejectFriendRequest(friendRequest: FriendRequest) = friendRequestManager.reject(friendRequest)
    fun deleteContact(publicKey: PublicKey) {
        callManager.endCall(publicKey)
        notificationHelper.dismissNotifications(publicKey)
        notificationHelper.dismissCallNotification(publicKey)
        contactManager.delete(publicKey)
        chatManager.clearHistory(publicKey)
        scope.launch {
            fileTransferManager.deleteAll(publicKey)
        }
    }

    fun contactAdded(toxId: PublicKey): Boolean = runBlocking {
        return@runBlocking contactManager.get(toxId).firstOrNull() != null
    }

    fun saveToxBackupTo(uri: Uri) = scope.launch(Dispatchers.IO) {
        // Export the save.
        try {
            resolver.openFileDescriptor(uri, "w")!!.use { fd ->
                FileOutputStream(fd.fileDescriptor).use { out ->
                    out.write(tox.getSaveData())
                }
            }
        } catch (_: FileNotFoundException) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.tox_save_export_failure, context.getString(R.string.file_not_found)),
                    Toast.LENGTH_LONG,
                ).show()
            }
            return@launch
        }

        // Verify that the exported save can be imported.
        resolver.openFileDescriptor(uri, "r")!!.use { fd ->
            FileInputStream(fd.fileDescriptor).use { ios ->
                val saveData = ios.readBytes()
                val save = SaveOptions(saveData, true, ProxyType.None, "", 0)
                val toast = when (val status = testToxSave(save, tox.password)) {
                    ToxSaveStatus.Ok -> context.getText(R.string.tox_save_exported)
                    else -> context.getString(R.string.tox_save_export_failure, status.name)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun onShareText(what: String, to: Contact) = chatManager.sendMessage(PublicKey(to.publicKey), what)
}

// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.createprofile

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import ltd.evilcorp.atox.tox.ToxStarter
import ltd.evilcorp.core.model.User
import ltd.evilcorp.domain.feature.UserManager
import ltd.evilcorp.domain.tox.Tox
import ltd.evilcorp.core.tox.save.ToxSaveStatus

class CreateProfileViewModel @Inject constructor(
    private val resolver: ContentResolver,
    private val userManager: UserManager,
    private val tox: Tox,
    private val toxStarter: ToxStarter,
) : ViewModel() {
    fun startTox(save: ByteArray? = null, password: String? = null): ToxSaveStatus = toxStarter.startTox(save, password)
    fun tryImportToxSave(uri: Uri): ByteArray? = resolver.openInputStream(uri)?.use { it.readBytes() }
    fun create(user: User) = userManager.create(user)

    fun createProfile(name: String): ToxSaveStatus {
        val status = startTox()
        if (status != ToxSaveStatus.Ok) {
            return status
        }

        create(User(publicKey = tox.publicKey.string(), name = name))
        return ToxSaveStatus.Ok
    }
}

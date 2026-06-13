// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.auth.repository

import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.auth.model.ProfileInfo

@Suppress("ComplexInterface")
interface IProfileRepository {
    suspend fun deleteProfile(publicKey: PublicKey)
    suspend fun clearDatabase()
    suspend fun createCheckpoint(): Boolean
    suspend fun restoreFromCheckpoint(): Boolean
    suspend fun clearCheckpoint()
    suspend fun finalizeProfileCreation(oldId: String, newId: String, name: String)

    fun getActiveProfileId(): String
    fun setActiveProfileId(id: String)
    fun getShowProfilePicker(): Boolean
    fun setShowProfilePicker(show: Boolean)
    fun getProfiles(): List<ProfileInfo>
    fun addOrUpdateProfile(profile: ProfileInfo)
    fun removeProfile(id: String)
}

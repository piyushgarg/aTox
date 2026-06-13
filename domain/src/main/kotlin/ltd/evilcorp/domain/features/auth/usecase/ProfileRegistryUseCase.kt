// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.auth.usecase

import ltd.evilcorp.domain.features.auth.model.ProfileInfo
import ltd.evilcorp.domain.features.auth.repository.IProfileRepository
import javax.inject.Inject

class ProfileRegistryUseCase @Inject constructor(
    private val profileRepository: IProfileRepository
) {
    fun getActiveProfileId(): String = profileRepository.getActiveProfileId()
    fun setActiveProfileId(id: String) = profileRepository.setActiveProfileId(id)
    fun getShowProfilePicker(): Boolean = profileRepository.getShowProfilePicker()
    fun setShowProfilePicker(show: Boolean) = profileRepository.setShowProfilePicker(show)
    fun getProfiles(): List<ProfileInfo> = profileRepository.getProfiles()
    fun addOrUpdateProfile(profile: ProfileInfo) = profileRepository.addOrUpdateProfile(profile)
    fun removeProfile(id: String) = profileRepository.removeProfile(id)
    suspend fun finalizeProfileCreation(oldId: String, newId: String, name: String) =
        profileRepository.finalizeProfileCreation(oldId, newId, name)
}

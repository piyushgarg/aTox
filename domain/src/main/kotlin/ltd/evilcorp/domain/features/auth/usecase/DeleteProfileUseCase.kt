// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.auth.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.domain.core.di.IoDispatcher
import ltd.evilcorp.domain.core.network.IToxProfile
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.features.auth.repository.IProfileRepository

private const val STOP_RETRY_DELAY_MS = 10L

class DeleteProfileUseCase @Inject constructor(
    private val tox: IToxProfile,
    private val profileDeleter: IProfileRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun execute() = withContext(ioDispatcher) {
        val pk = tox.publicKey
        val activeTox = tox as? ITox
        if (activeTox != null) {
            activeTox.stop()
            while (activeTox.started) {
                kotlinx.coroutines.delay(STOP_RETRY_DELAY_MS)
            }
        }
        profileDeleter.deleteProfile(pk)
    }
}

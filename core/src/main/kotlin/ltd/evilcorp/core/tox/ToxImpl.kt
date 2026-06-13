// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.runtime.ToxRuntime
import ltd.evilcorp.core.tox.impl.ToxProfileImpl
import ltd.evilcorp.core.tox.impl.ToxMessengerImpl
import ltd.evilcorp.core.tox.impl.ToxFileTransmitterImpl
import ltd.evilcorp.core.tox.impl.ToxCallControllerImpl
import ltd.evilcorp.core.tox.impl.ToxGroupManagerImpl
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.IToxProfile
import ltd.evilcorp.domain.core.network.IToxMessenger
import ltd.evilcorp.domain.core.network.IToxFileTransmitter
import ltd.evilcorp.domain.core.network.IToxCallController
import ltd.evilcorp.domain.core.network.IToxGroupManager

@Singleton
class ToxImpl @Inject constructor(
    private val runtime: ToxRuntime,
    private val profileImpl: ToxProfileImpl,
    private val messengerImpl: ToxMessengerImpl,
    private val fileTransmitterImpl: ToxFileTransmitterImpl,
    private val callControllerImpl: ToxCallControllerImpl,
    private val groupManagerImpl: ToxGroupManagerImpl,
) : ITox,
    IToxProfile by profileImpl,
    IToxMessenger by messengerImpl,
    IToxFileTransmitter by fileTransmitterImpl,
    IToxCallController by callControllerImpl,
    IToxGroupManager by groupManagerImpl {

    override var started: Boolean
        get() = runtime.started
        set(_) = Unit

    override var isBootstrapNeeded: Boolean
        get() = runtime.isBootstrapNeeded
        set(value) {
            runtime.isBootstrapNeeded = value
        }

    override val password: String?
        get() = runtime.password

    override val sessionId: String?
        get() = runtime.sessionId

    fun start(saveOption: SaveOptions, password: String?, listener: ToxEventListener, avListener: ToxAvEventListener) {
        runtime.start(saveOption, password, listener, avListener)
    }

    override fun changePassword(new: String?) = runtime.changePassword(new)

    override fun stop() {
        runtime.stop()
    }

    fun waitForStop() {
        runtime.waitForStop()
    }

    override fun getSaveData(): ByteArray = runtime.getSaveData()
}

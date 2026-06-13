// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox.impl

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.tox.runtime.ToxRuntime
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.IToxFileTransmitter
import ltd.evilcorp.domain.features.transfer.model.FileKind

@Singleton
class ToxFileTransmitterImpl @Inject constructor(
    private val runtime: ToxRuntime,
) : IToxFileTransmitter {
    override fun startFileTransfer(pk: PublicKey, fileNumber: Int) {
        runtime.startFileTransfer(pk, fileNumber)
    }

    override fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {
        runtime.stopFileTransfer(pk, fileNumber)
    }

    override fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int =
        runtime.sendFile(pk, fileKind, fileSize, fileName)

    override fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> =
        runtime.sendFileChunk(pk, fileNo, pos, data)
}

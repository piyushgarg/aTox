// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network

import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.transfer.model.FileKind

interface IToxFileTransmitter {
    fun startFileTransfer(pk: PublicKey, fileNumber: Int)
    fun stopFileTransfer(pk: PublicKey, fileNumber: Int)
    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int
    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit>
}

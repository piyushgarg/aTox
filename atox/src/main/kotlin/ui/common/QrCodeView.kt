// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import io.nayuki.qrcodegen.QrCode

@Composable
fun QrCodeView(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.Black
) {
    val qr = remember(text) {
        try {
            QrCode.encodeText(text, QrCode.Ecc.MEDIUM)
        } catch (e: Exception) {
            null
        }
    }

    if (qr != null) {
        Canvas(modifier = modifier) {
            val size = qr.size
            val cellSize = this.size.width / size
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (qr.getModule(x, y)) {
                        drawRect(
                            color = contentColor,
                            topLeft = Offset(x * cellSize, y * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

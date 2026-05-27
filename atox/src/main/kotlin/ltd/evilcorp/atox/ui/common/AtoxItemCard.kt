// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Suppress("FunctionNaming")
@Composable
fun AtoxItemCard(
    avatar: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    meta: (@Composable () -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val triggerHaptic = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    triggerHaptic()
                    onClick()
                },
                onLongClick = onLongClick?.let {
                    {
                        triggerHaptic()
                        it()
                    }
                }
            )
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            avatar()

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                title()
                Spacer(modifier = Modifier.height(4.dp))
                subtitle()
            }

            if (meta != null) {
                Spacer(modifier = Modifier.width(12.dp))
                meta()
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 76.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    }
}

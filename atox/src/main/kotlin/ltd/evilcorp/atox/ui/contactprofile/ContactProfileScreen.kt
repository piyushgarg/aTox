// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.contactprofile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.theme.StatusAvailable
import ltd.evilcorp.atox.ui.theme.StatusAway
import ltd.evilcorp.atox.ui.theme.StatusBusy
import ltd.evilcorp.atox.ui.userprofile.components.QrCodeDialog
import ltd.evilcorp.atox.ui.contactprofile.components.ContactIdShareCard
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.UserStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactProfileScreen(
    contact: Contact?,
    publicKey: String,
    onBack: () -> Unit = {},
    onShareContact: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showQrDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        contact?.name?.ifEmpty { context.getString(R.string.contact_default_name) }
                            ?: context.getString(R.string.contact_default_name),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Avatar with status indicator
            ContactAvatarBox(
                contact = contact,
                onAvatarClick = {
                    if (!contact?.avatarUri.isNullOrEmpty()) {
                        showAvatarDialog = true
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Status message Card (if exists)
            if (!contact?.statusMessage.isNullOrEmpty() && contact?.statusMessage != "...") {
                Card(
                    modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = stringResource(R.string.status_message),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = contact?.statusMessage ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Tox ID Card with Share Contact Button
            ContactIdShareCard(
                toxId = publicKey,
                onCopyClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Tox ID", publicKey)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.getString(R.string.profile_copied), Toast.LENGTH_SHORT).show()
                },
                onShareClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "tox:$publicKey")
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.tox_id_share)))
                },
                onQrClick = { showQrDialog = true },
                onShareContactClick = onShareContact,
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
            )

            val extraBottomSpacer = 0.dp
            Spacer(modifier = Modifier.height(extraBottomSpacer + bottomPadding))
        }
    }

    if (showQrDialog) {
        QrCodeDialog(
            toxId = publicKey,
            onDismiss = { showQrDialog = false }
        )
    }

    if (showAvatarDialog && !contact?.avatarUri.isNullOrEmpty()) {
        ltd.evilcorp.atox.ui.common.chat.FileImageViewerDialog(
            destination = contact?.avatarUri ?: "",
            fileName = "avatar.jpg",
            onDismiss = { showAvatarDialog = false }
        )
    }
}

@Composable
private fun ContactAvatarBox(
    contact: Contact?,
    onAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(100.dp)
            .clickable(enabled = !contact?.avatarUri.isNullOrEmpty()) { onAvatarClick() },
        contentAlignment = Alignment.BottomEnd
    ) {
        // Avatar
        ltd.evilcorp.atox.ui.common.ContactAvatar(
            name = contact?.name ?: "",
            publicKey = contact?.publicKey ?: "",
            avatarUri = contact?.avatarUri ?: "",
            size = 100.dp,
            fontSize = 36.sp
        )

        // Status indicator
        if (contact != null) {
            val statusColor = when (contact.status) {
                UserStatus.Away -> StatusAway
                UserStatus.Busy -> StatusBusy
                else -> StatusAvailable
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        CircleShape
                    )
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            statusColor,
                            CircleShape
                        )
                )
            }
        }
    }
}

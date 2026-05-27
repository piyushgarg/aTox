package ltd.evilcorp.atox.ui.contactlist.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.ContactAvatar
import androidx.compose.animation.ExperimentalSharedTransitionApi
import ltd.evilcorp.atox.ui.navigation.LocalSharedTransitionScope
import ltd.evilcorp.atox.ui.navigation.LocalAnimatedVisibilityScope
import ltd.evilcorp.atox.ui.common.PresenceText
import ltd.evilcorp.atox.ui.common.PresenceTone
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.atox.ui.common.formatPresenceText
import ltd.evilcorp.atox.ui.theme.StatusAvailable
import ltd.evilcorp.atox.ui.theme.StatusAway
import ltd.evilcorp.atox.ui.theme.StatusBusy
import ltd.evilcorp.atox.ui.theme.StatusOffline
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.DateFormatPreference
import ltd.evilcorp.domain.model.TimeFormatPreference

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItemCard(
    contact: Contact,
    dateFormatPreference: DateFormatPreference,
    timeFormatPreference: TimeFormatPreference,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val presence = remember(contact, dateFormatPreference, timeFormatPreference) {
        formatPresenceText(
            context = context,
            contact = contact,
            dateFormatPreference = dateFormatPreference,
            timeFormatPreference = timeFormatPreference,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete
            )
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatarWithStatus(contact = contact)

            Spacer(modifier = Modifier.width(16.dp))

            ContactTextBlock(
                contact = contact,
                presence = presence,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            ContactMetaBlock(
                contact = contact,
                timeFormatPreference = timeFormatPreference
            )
        }

        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 76.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
@Suppress("FunctionNaming")
private fun ContactAvatarWithStatus(contact: Contact) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    val avatarModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "avatar_${contact.publicKey}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        Modifier
    }

    Box(modifier = Modifier.size(48.dp)) {
        ContactAvatar(
            name = contact.name,
            publicKey = contact.publicKey,
            avatarUri = contact.avatarUri,
            size = 48.dp,
            fontSize = 18.sp,
            modifier = Modifier.fillMaxSize().then(avatarModifier)
        )

        val statusColor = when (contact.connectionStatus) {
            ConnectionStatus.None -> StatusOffline
            ConnectionStatus.TCP, ConnectionStatus.UDP -> when (contact.status) {
                ltd.evilcorp.domain.model.UserStatus.None -> StatusAvailable
                ltd.evilcorp.domain.model.UserStatus.Away -> StatusAway
                ltd.evilcorp.domain.model.UserStatus.Busy -> StatusBusy
            }
        }

        Box(
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(statusColor)
            )
        }
    }
}

@Composable
private fun ContactTextBlock(
    contact: Contact,
    presence: PresenceText,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = contact.name.ifEmpty { stringResource(R.string.contact_default_name) },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        when {
            contact.typing -> TypingPreviewText()
            contact.draftMessage.isNotEmpty() -> Text(
                text = stringResource(R.string.draft_message, contact.draftMessage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            else -> Text(
                text = presence.text,
                style = MaterialTheme.typography.bodyMedium,
                color = when (presence.color) {
                    PresenceTone.Online -> StatusAvailable
                    PresenceTone.Away -> StatusAway
                    PresenceTone.Busy -> StatusBusy
                    PresenceTone.Accent -> MaterialTheme.colorScheme.primary
                    PresenceTone.Muted -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TypingPreviewText() {
    Text(
        text = stringResource(R.string.contact_typing),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun ContactMetaBlock(
    contact: Contact,
    timeFormatPreference: TimeFormatPreference,
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        val dateText = remember(contact.lastMessage, timeFormatPreference) {
            if (contact.lastMessage != 0L) {
                formatChatTime(context, contact.lastMessage, timeFormatPreference)
            } else {
                ""
            }
        }

        Text(
            text = dateText,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (contact.hasUnreadMessages) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary)
                )
            }
        }
    }
}

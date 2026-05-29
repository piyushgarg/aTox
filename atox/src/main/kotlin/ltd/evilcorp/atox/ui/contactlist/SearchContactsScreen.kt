// SPDX-FileCopyrightText: 2026 aTox contributors
// SPDX-License-Identifier: GPL-3.0-only
package ltd.evilcorp.atox.ui.contactlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.domain.features.contacts.model.Contact
import androidx.compose.ui.graphics.toArgb

@Suppress("FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContactsScreen(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onBack: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) {
                return@remember ctx
            }
            ctx = ctx.baseContext
        }
        null
    }
    val statusBarColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val originalColor = remember(activity) { activity?.window?.statusBarColor ?: 0 }

    androidx.compose.runtime.DisposableEffect(statusBarColor) {
        activity?.window?.let { window ->
            window.statusBarColor = statusBarColor.toArgb()
        }
        onDispose {
            activity?.window?.let { window ->
                window.statusBarColor = originalColor
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                ltd.evilcorp.atox.ui.common.AtoxSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {},
                    active = true,
                    onActiveChange = { active ->
                        if (!active) {
                            onBack()
                        }
                    },
                    placeholder = stringResource(R.string.contact_list_search_placeholder),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    val filteredContacts = remember(searchQuery, contacts) {
                        if (searchQuery.isBlank()) emptyList()
                        else contacts.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.publicKey.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredContacts.isEmpty() && searchQuery.isNotBlank()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.search_no_results),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(
                                items = filteredContacts,
                                key = { contact -> contact.publicKey }
                            ) { contact ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = contact.name.ifEmpty { stringResource(R.string.contact_default_name) },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = contact.publicKey,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingContent = {
                                        ContactAvatar(
                                            name = contact.name.ifEmpty { stringResource(R.string.contact_default_name) },
                                            publicKey = contact.publicKey,
                                            avatarUri = contact.avatarUri,
                                            size = 40.dp,
                                            fontSize = 16.sp
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onContactClick(contact)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

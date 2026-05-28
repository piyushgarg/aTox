// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.common.AtoxSearchBar
import ltd.evilcorp.atox.ui.contactlist.components.ContactItemCard
import ltd.evilcorp.domain.features.contacts.model.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardSelectionScreen(
    contacts: List<Contact>,
    settings: Settings,
    onBack: () -> Unit,
    onContactsSelect: (List<Contact>) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }

    // Multi-select state
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.publicKey.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val selectedContacts = remember(contacts, selectedKeys) {
        contacts.filter { it.publicKey in selectedKeys }
    }

    Scaffold(
        topBar = {
            if (!isSearching) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.forward_message_title),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isSearching) {
                AtoxSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    active = isSearching,
                    onActiveChange = { active ->
                        isSearching = active
                        if (!active) {
                            searchQuery = ""
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    },
                    placeholder = stringResource(R.string.contact_list_search_placeholder)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        items(filteredContacts, key = { it.publicKey }) { contact ->
                            val isSelected = contact.publicKey in selectedKeys
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedKeys = if (isSelected) {
                                            selectedKeys - contact.publicKey
                                        } else {
                                            selectedKeys + contact.publicKey
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedKeys = if (checked == true) {
                                            selectedKeys + contact.publicKey
                                        } else {
                                            selectedKeys - contact.publicKey
                                        }
                                    },
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                                ContactItemCard(
                                    contact = contact,
                                    dateFormatPreference = settings.dateFormatPreference,
                                    timeFormatPreference = settings.timeFormatPreference,
                                    onClick = {
                                        selectedKeys = if (isSelected) {
                                            selectedKeys - contact.publicKey
                                        } else {
                                            selectedKeys + contact.publicKey
                                        }
                                    },
                                    onDelete = {},
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.contacts_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredContacts, key = { it.publicKey }) { contact ->
                            val isSelected = contact.publicKey in selectedKeys
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedKeys = if (isSelected) {
                                            selectedKeys - contact.publicKey
                                        } else {
                                            selectedKeys + contact.publicKey
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedKeys = if (checked == true) {
                                            selectedKeys + contact.publicKey
                                        } else {
                                            selectedKeys - contact.publicKey
                                        }
                                    },
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                                ContactItemCard(
                                    contact = contact,
                                    dateFormatPreference = settings.dateFormatPreference,
                                    timeFormatPreference = settings.timeFormatPreference,
                                    onClick = {
                                        selectedKeys = if (isSelected) {
                                            selectedKeys - contact.publicKey
                                        } else {
                                            selectedKeys + contact.publicKey
                                        }
                                    },
                                    onDelete = {},
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (selectedKeys.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            onContactsSelect(selectedContacts)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Forward (${selectedKeys.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

package ltd.evilcorp.atox.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.contactlist.components.ContactItemCard
import ltd.evilcorp.core.model.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardSelectionScreen(
    contacts: List<Contact>,
    settings: Settings,
    onBack: () -> Unit,
    onContactSelect: (Contact) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }

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

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (isSearching) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = {},
                            expanded = true,
                            onExpandedChange = { 
                                if (!it) {
                                    isSearching = false
                                    searchQuery = ""
                                }
                            },
                            placeholder = { Text(stringResource(R.string.contact_list_search_placeholder)) },
                            leadingIcon = {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    isSearching = false
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    },
                    expanded = true,
                    onExpandedChange = {
                        if (!it) {
                            isSearching = false
                            searchQuery = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    content = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            items(filteredContacts, key = { it.publicKey }) { contact ->
                                ContactItemCard(
                                    contact = contact,
                                    dateFormatPreference = settings.dateFormatPreference,
                                    timeFormatPreference = settings.timeFormatPreference,
                                    onClick = { onContactSelect(contact) },
                                    onDelete = {}
                                )
                            }
                        }
                    }
                )
            } else {
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
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
                        ContactItemCard(
                            contact = contact,
                            dateFormatPreference = settings.dateFormatPreference,
                            timeFormatPreference = settings.timeFormatPreference,
                            onClick = { onContactSelect(contact) },
                            onDelete = {}
                        )
                    }
                }
            }
        }
    }
}

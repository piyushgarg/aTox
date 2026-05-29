// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import androidx.compose.ui.graphics.toArgb

@Suppress("FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSearchPopup(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchItems: List<SearchableSetting>,
    onDismissRequest: () -> Unit,
    performHaptic: () -> Unit,
    onItemClick: (SearchableSetting) -> Unit
) {
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
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                ltd.evilcorp.atox.ui.common.AtoxSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onSearch = {},
                    active = true,
                    onActiveChange = { active ->
                        if (!active) {
                            onDismissRequest()
                        }
                    },
                    placeholder = stringResource(R.string.search_settings),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    val filteredItems = remember(searchQuery, searchItems) {
                        if (searchQuery.isBlank()) {
                            emptyList()
                        } else {
                            searchItems.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.subtitle.contains(searchQuery, ignoreCase = true) ||
                                it.category.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredItems.isEmpty()) {
                            if (searchQuery.isNotBlank()) {
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
                            }
                        } else {
                            items(filteredItems) { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            performHaptic()
                                            onItemClick(item)
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
            }
        }
    }
}

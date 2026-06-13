package ltd.evilcorp.atox.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ltd.evilcorp.atox.R

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtoxSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.contact_list_search_placeholder),
    trailingIcon: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                expanded = active,
                onExpandedChange = onActiveChange,
                placeholder = { Text(placeholder) },
                leadingIcon = {
                    MorphingNavigationIcon(
                        isBack = active,
                        onClick = { onActiveChange(!active) }
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear"
                            )
                        }
                    } else {
                        trailingIcon?.invoke()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        expanded = active,
        onExpandedChange = onActiveChange,
        colors = SearchBarDefaults.colors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth().semantics {
            contentDescription = "AtoxSearchBar"
        },
        content = { content() }
    )
}

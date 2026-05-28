package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.addcontact.AddContactScreen
import ltd.evilcorp.atox.ui.addcontact.AddContactViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.navigation.LocalTabPadding
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.addContactTabRoute(
    navController: NavHostController
) {
    composable<AppRoutes.AddContactTab> {
        val context = LocalContext.current
        val addContactViewModel: AddContactViewModel = hiltViewModel()
        val user by addContactViewModel.user.collectAsStateWithLifecycle()
        val connectionStatus = user?.connectionStatus ?: ConnectionStatus.None

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = context.getString(R.string.add_contact),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (connectionStatus == ConnectionStatus.None) {
                                Text(
                                    text = context.getString(R.string.connecting),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(R.string.navigation_back)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = LocalTabPadding.current.calculateBottomPadding())
            ) {
                AddContactScreen(
                    viewModel = addContactViewModel,
                    showBackButton = false,
                    onSuccess = {
                        navController.navigate(AppRoutes.Chats) {
                            launchSingleTop = true
                            popUpTo(AppRoutes.Chats)
                        }
                    }
                )
            }
        }
    }
}

package ltd.evilcorp.atox.ui.profilepicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.domain.features.auth.model.ProfileInfo
import ltd.evilcorp.core.profile.ProfileManager
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.ContactAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePickerScreen(
    onProfileSelected: () -> Unit,
    onCreateProfile: () -> Unit
) {
    val context = LocalContext.current
    android.util.Log.d("AtoxNav", "ProfilePickerScreen composed. Profiles size: ${ProfileManager.getProfiles(context).size}")
    val profiles = remember { mutableStateOf(ProfileManager.getProfiles(context)) }
    val scope = rememberCoroutineScope()
    var profileToDelete by remember { mutableStateOf<ProfileInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(R.string.profile_picker_title)) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateProfile,
                text = { Text(androidx.compose.ui.res.stringResource(R.string.profile_picker_add)) },
                icon = { /* Add icon here */ }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(profiles.value) { profile ->
                ProfileItemCard(
                    profile = profile,
                    onClick = {
                        ProfileManager.setActiveProfileId(context, profile.id)
                        ProfileManager.setShowProfilePicker(context, false)
                        onProfileSelected()
                    },
                    onDelete = {
                        profileToDelete = profile
                    }
                )
            }
        }
    }

    if (profileToDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text(androidx.compose.ui.res.stringResource(R.string.profile_delete_confirm_title)) },
            text = { Text(androidx.compose.ui.res.stringResource(R.string.profile_delete_confirm_desc, profileToDelete?.name ?: "")) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val profile = profileToDelete
                        if (profile != null) {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    // 1. Delete Tox file
                                    File(context.filesDir, "${profile.id}.tox").delete()
                                    
                                    // 2. Delete Database files
                                    val dbFile = context.getDatabasePath("core_db_${profile.id}")
                                    if (dbFile.exists()) dbFile.delete()
                                    File(dbFile.path + "-wal").delete()
                                    File(dbFile.path + "-shm").delete()
                                    
                                    // 3. Delete DataStore preferences file
                                    File(context.filesDir, "datastore/user_settings_${profile.id}.preferences_pb").delete()
                                    
                                    // 4. Delete Avatars
                                    File(context.filesDir, "self_avatar_${profile.id}.jpg").delete()
                                    File(context.filesDir, "self_avatar_${profile.id}.png").delete()
                                    
                                    // 5. Remove from ProfileManager
                                    ProfileManager.removeProfile(context, profile.id)
                                }
                                // Refresh profiles list
                                profiles.value = ProfileManager.getProfiles(context)
                                profileToDelete = null
                            }
                        }
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(R.string.delete))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { profileToDelete = null }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.profile_logout_cancel_button))
                }
            }
        )
    }
}

@Composable
fun ProfileItemCard(
    profile: ProfileInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ContactAvatar(
                name = profile.name,
                publicKey = profile.id, // stable color seed
                avatarUri = profile.avatarUri ?: "",
                size = 40.dp,
                fontSize = 16.sp
            )

            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = androidx.compose.ui.res.stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

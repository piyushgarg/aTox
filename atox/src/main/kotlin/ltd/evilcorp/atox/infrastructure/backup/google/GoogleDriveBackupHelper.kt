@file:Suppress("DEPRECATION")
package ltd.evilcorp.atox.infrastructure.backup.google

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import ltd.evilcorp.domain.features.backup.model.CloudBackupInfo
import ltd.evilcorp.domain.features.backup.repository.ICloudBackupRepository

private const val KB_IN_MB = 1024L

class GoogleDriveBackupHelper @Inject constructor(
    private val context: Context
) : ICloudBackupRepository {

    fun getSignInIntent() = getSignInClient().signInIntent

    fun getSignInClient(): GoogleSignInClient {
        return GoogleSignIn.getClient(context, getSignInOptions())
    }

    suspend fun getAccount(): GoogleSignInAccount? = withContext(Dispatchers.IO) {
        val signInOptions = getSignInOptions()
        val client = GoogleSignIn.getClient(context, signInOptions)
        try {
            com.google.android.gms.tasks.Tasks.await(client.silentSignIn())
        } catch (e: Exception) {
            Log.w("GoogleDriveBackupHelper", "silentSignIn failed, falling back to getLastSignedInAccount", e)
            val account = GoogleSignIn.getLastSignedInAccount(context)
            Log.d("GoogleDriveBackupHelper", "getLastSignedInAccount returned: $account (email: ${account?.email})")
            if (account != null) {
                val grantedScopes = account.grantedScopes
                Log.d("GoogleDriveBackupHelper", "Granted scopes: $grantedScopes")
                val hasFileScope = GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))
                val hasAppDataScope = GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))
                Log.d("GoogleDriveBackupHelper", "hasPermissions: DRIVE_FILE=$hasFileScope, DRIVE_APPDATA=$hasAppDataScope")
                
                // Return the account if not null. Even if hasPermissions returns false, we still return it
                // to allow the Drive API request to proceed and fail with a clear exception (if any),
                // instead of preemptively returning null and aborting.
                account
            } else {
                Log.e("GoogleDriveBackupHelper", "No account available from getLastSignedInAccount")
                null
            }
        }
    }

    companion object {
        fun getSignInOptions(): GoogleSignInOptions {
            return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
                .build()
        }
    }


    suspend fun uploadBackup(fileBytes: ByteArray, filename: String): Long {
        val account = getAccount() ?: error("Not signed in")
        return withContext(Dispatchers.IO) {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccountName = account.email

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("aTox")
                .build()

            // Find existing backup file in AppData folder
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$filename'")
                .setFields("files(id, size)")
                .execute()

            val existingFile = fileList.files.firstOrNull()
            
            val content = ByteArrayContent("application/zip", fileBytes)
            
            val uploadedFile = if (existingFile != null) {
                // Update existing file
                Log.d("GoogleDrive", "Updating existing backup file ID: ${existingFile.id}")
                driveService.files().update(existingFile.id, File(), content).execute()
            } else {
                // Create new file
                Log.d("GoogleDrive", "Creating new backup file in AppDataFolder")
                val fileMetadata = File().apply {
                    name = filename
                    parents = listOf("appDataFolder")
                }
                driveService.files().create(fileMetadata, content).execute()
            }
            
            Log.d("GoogleDrive", "Uploaded backup to Google Drive successfully. ID: ${uploadedFile.id}")
            
            // Return size in KB
            val fileSize = uploadedFile.size ?: java.lang.Long.valueOf(fileBytes.size.toLong())
            fileSize.toLong() / KB_IN_MB
        }
    }

    override suspend fun listBackups(): List<CloudBackupInfo> {
        return withContext(Dispatchers.IO) {
            val account = getAccount() ?: return@withContext emptyList()
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccountName = account.email

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("aTox")
                .build()

            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name contains 'atox_backup'") // Optional filter
                .setFields("files(id, name, size, createdTime)")
                .execute()

            fileList.files?.map {
                CloudBackupInfo(
                    id = it.id,
                    name = it.name,
                    sizeKb = (it.getSize() ?: 0L) / KB_IN_MB,
                    createdTimeMs = it.createdTime?.value ?: 0L
                )
            } ?: emptyList()
        }
    }

    override suspend fun downloadBackup(fileId: String): ByteArray {
        return withContext(Dispatchers.IO) {
            val account = getAccount() ?: error("Not signed in")
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccountName = account.email

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("aTox")
                .build()

            val outputStream = ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.toByteArray()
        }
    }
}

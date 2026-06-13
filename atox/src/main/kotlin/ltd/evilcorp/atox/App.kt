package ltd.evilcorp.atox

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), androidx.work.Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        logSigningCertificateFingerprint()

        // Background cleanup of shared cache files on startup to prevent leaks
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val sharedCacheDir = File(cacheDir, "shared")
                if (sharedCacheDir.exists() && sharedCacheDir.isDirectory) {
                    sharedCacheDir.listFiles()?.forEach { file ->
                        file.delete()
                    }
                }
            }
        }
    }

    private fun logSigningCertificateFingerprint() {
        try {
            val packageName = packageName
            val packageManager = packageManager
            @Suppress("DEPRECATION")
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                packageInfo.signatures
            }

            signatures?.forEach { signature ->
                val md = MessageDigest.getInstance("SHA-1")
                val publicKey = md.digest(signature.toByteArray())
                val hexString = publicKey.joinToString(":") { String.format(java.util.Locale.US, "%02X", it) }
                Log.e("BackupDiagnostics", "================ BACKUP DIAGNOSTICS ================")
                Log.e("BackupDiagnostics", "App Package Name: $packageName")
                Log.e("BackupDiagnostics", "SHA-1 Fingerprint: $hexString")
                Log.e("BackupDiagnostics", "Ensure this SHA-1 and package name are registered in your Google Cloud Console Android Client ID!")
                Log.e("BackupDiagnostics", "====================================================")
            }
        } catch (e: Exception) {
            Log.e("BackupDiagnostics", "Failed to get signing certificate fingerprint", e)
        }
    }
}

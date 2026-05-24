package ltd.evilcorp.atox.ui.createprofile

interface ProfileBackupProcessor {
    suspend fun readBackupBytes(uriString: String): ByteArray?
}

package ltd.evilcorp.domain.features.settings

interface ISettingsFileProcessor {
    suspend fun readBytes(uriString: String): ByteArray?
    suspend fun writeBytes(uriString: String, bytes: ByteArray): Boolean
    suspend fun saveUserNodesJson(bytes: ByteArray): Boolean
}

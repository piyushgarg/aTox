package ltd.evilcorp.domain.core.network.save

/**
 * Enum of possible loading and validation statuses of the Tox binary save file (save data).
 */
enum class ToxSaveStatus {
    /** Successful loading and validation. */
    Ok,
    /** Invalid format of the save file. */
    BadFormat,
    /** The file is encrypted (password required). */
    Encrypted,
    /** Memory allocation error (Out of Memory). */
    OutOfMemory,
    /** Null save data. */
    Null,
    /** Socket port allocation error. */
    PortAlloc,
    /** Invalid proxy host. */
    BadProxyHost,
    /** Invalid proxy port. */
    BadProxyPort,
    /** Unsupported proxy type. */
    BadProxyType,
    /** Proxy server not found or unreachable. */
    ProxyNotFound,
    /** Save file not found. */
    SaveNotFound,
}

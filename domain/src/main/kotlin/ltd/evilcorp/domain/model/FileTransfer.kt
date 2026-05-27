package ltd.evilcorp.domain.model

enum class FileKind {
    Data,
    Avatar,
}

// Since the progress can't be negative, I'm reusing that part for some markers.
const val FT_STARTED = 0L
const val FT_NOT_STARTED = -1L
const val FT_REJECTED = -2L

data class FileTransfer(
    val publicKey: String,
    val fileNumber: Int,
    val fileKind: Int,
    val fileSize: Long,
    val fileName: String,
    val outgoing: Boolean,
    var progress: Long = FT_NOT_STARTED,
    var destination: String = "",
    var id: Int = 0,
)

fun FileTransfer.isComplete() = progress >= fileSize
fun FileTransfer.isStarted() = progress >= FT_STARTED
fun FileTransfer.isRejected() = progress == FT_REJECTED

package ltd.evilcorp.domain.core.network

internal object Log {
    fun d(tag: String, msg: String) = println("[$tag] D: $msg")
    fun i(tag: String, msg: String) = println("[$tag] I: $msg")
    fun w(tag: String, msg: String) = System.err.println("[$tag] W: $msg")
    fun e(tag: String, msg: String) = System.err.println("[$tag] E: $msg")
}

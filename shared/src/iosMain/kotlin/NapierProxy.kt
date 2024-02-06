import io.github.aakira.napier.Napier

/**
 * Proxy class to easy access to Napier methods from Swift code
 */
class NapierProxy {
    companion object {
        fun i(msg: String) {
            Napier.i(msg)
        }

        fun e(msg: String) {
            Napier.e(msg)
        }

        fun w(msg: String) {
            Napier.w(msg)
        }

        fun d(msg: String) {
            Napier.d(msg)
        }
    }
}
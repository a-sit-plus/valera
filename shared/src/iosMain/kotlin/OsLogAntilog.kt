/*import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.LogLevel.*
import platform.Foundation.NSLog

class OsLogAntilog : Antilog() {
    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?
    ) {
        val level = when (priority) {
            VERBOSE, DEBUG -> "DEBUG"
            INFO -> "INFO"
            WARNING -> "WARN"
            ERROR, ASSERT -> "ERROR"
        }

        val tagPart = tag?.let { "[$it]" } ?: ""
        val errorPart = throwable?.let { " | ${it.message ?: it.toString()}" } ?: ""
        val finalMessage = "[$level]$tagPart ${message.orEmpty()}$errorPart"

        //NSLog("%@", finalMessage)
    }
}*/








/*import io.github.aakira.napier.Antilog
import OsLogBridge  // from the Swift-generated header
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.LogLevel.*

class OsLogAntilog : Antilog() {
    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?
    ) {
        val levelString = when (priority) {
            VERBOSE, DEBUG -> "DEBUG"
            INFO -> "INFO"
            WARNING -> "WARN"
            ERROR, ASSERT -> "ERROR"
        }

        val base = buildString {
            append(message.orEmpty())
            throwable?.let {
                if (isNotEmpty()) append(" | ")
                append(it.message ?: it.toString())
            }
        }

        OsLogBridge.log(
            level = levelString,
            tag = tag,
            message = base
        )
    }

}*/
package data.storage

import at.asitplus.wallet.app.common.PlatformAdapter
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.LogLevel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AntilogAdapter(val platformAdapter: PlatformAdapter, private val defaultTag: String): Antilog() {
    val debugAntilogAdapter = DebugAntilog(defaultTag = defaultTag)

    /**
     * Modified from DebugAntiLog
     */
    override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
        val now: Instant = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = date.hour.toString().padStart(2, '0')
        val minute = date.minute.toString().padStart(2, '0')
        val time = "${hour}:${minute}"



        val logTag = tag ?: defaultTag

        val fullMessage = if (message != null) {
            if (throwable != null) {
                "$message\n${throwable.message}"
            } else {
                message
            }
        } else throwable?.message ?: return

        val pad = 13

        when (priority) {
            LogLevel.VERBOSE -> {
                val info = "$time VERBOSE".padEnd(pad, ' ')
                platformAdapter.writeToLog("$info $logTag : $fullMessage\n\n")
            }
            LogLevel.DEBUG -> {
                val info = "$time DEBUG".padEnd(pad, ' ')
                platformAdapter.writeToLog("$info $logTag : $fullMessage\n\n")
            }
            LogLevel.INFO -> {
                val info = "$time INFO".padEnd(pad, ' ')
                platformAdapter.writeToLog("$info $logTag : $fullMessage\n\n")
            }
            LogLevel.WARNING -> {
                val info = "$time WARNING".padEnd(pad, ' ')
                platformAdapter.writeToLog("$info $logTag : $fullMessage\n\n")
            }
            LogLevel.ERROR -> {
                val info = "$time ERROR".padEnd(pad, ' ')
                val message = throwable?.message ?: "Unknown Message"
                val cause = throwable?.cause?.message ?: "Unknown Cause"
                val stackTrace = throwable?.stackTraceToString()
                platformAdapter.writeToLog("$info $logTag : $message, $cause\n$stackTrace\n\n")
            }
            LogLevel.ASSERT -> {
                val info = "$time ASSERT".padEnd(pad, ' ')
                platformAdapter.writeToLog("$info $logTag : $fullMessage\n\n")
            }
        }

        debugAntilogAdapter.log(priority, tag, throwable, message)
    }
}
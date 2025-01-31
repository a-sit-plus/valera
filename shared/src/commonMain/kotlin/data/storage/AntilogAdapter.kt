package data.storage

import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.PlatformAdapter
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.LogLevel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AntilogAdapter(val platformAdapter: PlatformAdapter, private val defaultTag: String, private val buildType: BuildType): Antilog() {
    private val debugAntilogAdapter = DebugAntilog(defaultTag = defaultTag)

    /**
     * Modified from DebugAntiLog
     */
    override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {

        if ( buildType == BuildType.RELEASE && (priority == LogLevel.VERBOSE || priority == LogLevel.DEBUG) ) {
            return
        }

        val now: Instant = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = date.hour.toString().padStart(2, '0')
        val minute = date.minute.toString().padStart(2, '0')
        val time = "${hour}:${minute}"
        val logTag = tag ?: defaultTag
        val pad = 15
        
        val message = message ?: ""

        val data: String

        when (priority) {
            LogLevel.ERROR -> {
                val message = throwable?.message ?: "Unknown Message"
                val cause = throwable?.cause?.message ?: "Unknown Cause"
                val stackTrace = throwable?.stackTraceToString() ?: ""
                val info = "[${time}] ERROR".padEnd(pad, ' ')
                data = ("$info $logTag : $message, $cause\n$stackTrace\n")
            }
            else -> {
                val info = "[${time}] ${priority}".padEnd(pad, ' ')
                data = ("$info $logTag : $message\n")
            }
        }

        platformAdapter.writeToFile(data, "log.txt", "logs")
        debugAntilogAdapter.log(priority, tag, throwable, message)
    }
}
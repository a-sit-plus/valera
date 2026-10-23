package data.storage

import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.PlatformAdapter
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.LogLevel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

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
        val info = "[${time}] ${priority}".padEnd(pad, ' ')
        val stackTrace = throwable?.let { "${it.stackTraceToString()}\n" } ?: ""
        val data = "$info $logTag : $message\n$stackTrace"

        platformAdapter.writeToFile(data, "log.txt", "logs")
        debugAntilogAdapter.log(priority, tag, throwable, message)
    }
}
package data.storage

import at.asitplus.wallet.app.common.PlatformAdapter
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AntilogAdapter(val platformAdapter: PlatformAdapter): Antilog() {
    override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
        val now: Instant = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = date.hour.toString().padStart(2, '0')
        val minute = date.minute.toString().padStart(2, '0')
        val time = "${hour}:${minute}"
        platformAdapter.writeToLog("$time $priority $tag $message\n")
    }
}
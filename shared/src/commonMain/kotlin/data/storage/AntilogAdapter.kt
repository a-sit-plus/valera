package data.storage

import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.PlatformAdapter
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.LogLevel
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

class AntilogAdapter(val platformAdapter: PlatformAdapter, private val defaultTag: String, private val buildType: BuildType): Antilog() {
    private val debugAntilogAdapter = DebugAntilog(defaultTag = defaultTag)

    /**
     * Lines not yet written to the log file. Appending to it on the logging thread blocked e.g. the main thread for
     * every line, so lines are written in order by [flush], from one background coroutine for the whole process.
     */
    private val pending = ArrayDeque<String>()
    private val pendingLock = SynchronizedObject()
    private val writeLock = SynchronizedObject()
    private val wakeUp = Channel<Unit>(Channel.CONFLATED)

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1)).launch {
            for (signal in wakeUp) flush()
        }
    }

    /** Appends the pending lines to the log file, in the order they were logged. */
    private fun flush() = synchronized(writeLock) {
        val text = synchronized(pendingLock) {
            pending.joinToString("").also { pending.clear() }
        }
        if (text.isNotEmpty()) platformAdapter.writeToFile(text, "log.txt", "logs")
    }

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

        synchronized(pendingLock) { pending.addLast(data) }
        // An error is written at once, with the lines before it, so that it is in the file also if the app crashes
        if (priority >= LogLevel.ERROR) flush() else wakeUp.trySend(Unit)
        debugAntilogAdapter.log(priority, tag, throwable, message)
    }
}
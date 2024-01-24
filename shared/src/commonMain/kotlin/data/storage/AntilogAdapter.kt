package data.storage

import at.asitplus.wallet.lib.data.jsonSerializer
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.LogLevel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class AntilogAdapter(val dataStoreService: DataStoreService, private val defaultTag: String): Antilog() {
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

        val message = message ?: ""

        when (priority) {
            LogLevel.ERROR -> {
                val message = throwable?.message ?: "Unknown Message"
                val cause = throwable?.cause?.message ?: "Unknown Cause"
                val stackTrace = throwable?.stackTraceToString() ?: ""
                val data = jsonSerializer.encodeToString(logDataError(message, cause, stackTrace))
                val export = exportLog(time, priority,logTag, data)
                dataStoreService.writeLogToFile(export)
            }
            else -> {
                val data = jsonSerializer.encodeToString(logDataGeneral(message))
                val export = exportLog(time, priority,logTag, data)
                dataStoreService.writeLogToFile(export)
            }
        }
        debugAntilogAdapter.log(priority, tag, throwable, message)
    }
}

@Serializable
data class exportLog(val time: String, val priority: LogLevel, val logTag: String, val data: String)
@Serializable
data class logDataGeneral(val message: String)
@Serializable
data class logDataError(val message: String, val cause: String, val stackTrace: String)

fun MutableList<exportLog>.stringify():MutableList<String> {
    val pad = 13
    val stringArray = mutableListOf<String>()
    this.forEach {
        try {
            when (it.priority){
                LogLevel.ERROR -> {
                    val data = at.asitplus.wallet.lib.oidvci.jsonSerializer.decodeFromString<logDataError>(it.data)
                    val info = "${it.time} VERBOSE".padEnd(pad, ' ')
                    val logTag = it.logTag
                    val message = data.message
                    val cause = data.cause
                    val stackTrace = data.stackTrace
                    stringArray.add("$info $logTag : $message, $cause\n$stackTrace\n\n")
                }
                else -> {
                    val data = at.asitplus.wallet.lib.oidvci.jsonSerializer.decodeFromString<logDataGeneral>(it.data)
                    val info = "${it.time} ${it.priority}".padEnd(pad, ' ')
                    val logData = it.logTag
                    val message = data.message
                    stringArray.add("$info $logData : $message")}
            }
        } catch(e: Throwable){
            println(e)
        }
    }
    return stringArray
}
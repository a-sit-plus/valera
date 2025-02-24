package data.bletransfer.logging

import com.android.identity.util.Logger
import io.github.aakira.napier.Napier

class CustomAndroidIdentityLogPrinter : Logger.LogPrinter {
    override fun printLn(level: Int, tag: String, msg: String, throwable: Throwable?) {
        when (level) {
            Logger.LEVEL_D -> Napier.d(tag = tag, throwable = throwable, message = msg)
            Logger.LEVEL_I -> Napier.i(tag = tag, throwable = throwable, message = msg)
            Logger.LEVEL_W -> Napier.w(tag = tag, throwable = throwable, message = msg)
            Logger.LEVEL_E -> Napier.e(tag = tag, throwable = throwable, message = msg)
            else -> Napier.d(tag = tag, throwable = throwable, message = msg)
        }
    }
}

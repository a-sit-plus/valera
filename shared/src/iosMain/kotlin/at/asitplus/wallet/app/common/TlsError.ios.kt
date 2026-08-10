package at.asitplus.wallet.app.common

import io.ktor.client.engine.darwin.DarwinHttpRequestException

internal actual fun Throwable.isPlatformTlsError(): Boolean =
    (this as? DarwinHttpRequestException)?.origin?.code?.toLong() in -1206L..-1200L

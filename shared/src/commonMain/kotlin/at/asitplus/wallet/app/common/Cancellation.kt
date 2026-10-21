package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.catching
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs [block], and rethrows a [CancellationException] that does not cancel the calling coroutine as an
 * [IllegalStateException], e.g. of a request whose job was cancelled by its HTTP client, or of a shared refresh
 * cancelled by another session. `catching` rethrows every [CancellationException], which would end the caller as
 * if it was cancelled, e.g. leave a screen loading forever, or escape to the collector of a flow.
 */
suspend inline fun <T> failOnForeignCancellation(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    currentCoroutineContext().ensureActive()
    throw IllegalStateException("Cancelled without cancelling the caller: ${e.message}", e)
}

/**
 * [catching], that fails as well on a [CancellationException] of [block] unless the caller is cancelled, see
 * [failOnForeignCancellation].
 */
suspend inline fun <T> catchingUnlessCancelled(block: () -> T): KmmResult<T> =
    catching { failOnForeignCancellation(block) }

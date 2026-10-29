package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.wallet.app.common.catchingUnlessCancelled
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The [validation] of a relying party to show, which is replaced once the status of its registration certificates
 * (WRPRCs) is resolved in the background, while [isStatusPending]. A status that can not be resolved stays as not
 * checked.
 */
class RelyingPartyStatusResolution private constructor(
    val validation: StateFlow<RelyingPartyValidationUi?>,
    /** Whether the status of a WRPRC of the relying party is still being retrieved. */
    val isStatusPending: StateFlow<Boolean>,
    private val job: Job?,
) {
    /** Stops resolving the status, e.g. once the request is left. */
    fun cancel() {
        job?.cancel()
    }

    companion object {
        /** A [validation] without any status to resolve. */
        fun resolved(validation: RelyingPartyValidationUi?) = RelyingPartyStatusResolution(
            validation = MutableStateFlow(validation).asStateFlow(),
            isStatusPending = MutableStateFlow(false).asStateFlow(),
            job = null,
        )

        /**
         * Shows [initial] until [resolve], started in [scope] off the UI dispatcher if [isPending], returns the
         * validation with the status of the WRPRCs.
         */
        fun start(
            initial: RelyingPartyValidationUi?,
            isPending: Boolean,
            scope: CoroutineScope,
            resolve: suspend () -> RelyingPartyValidationUi?,
        ): RelyingPartyStatusResolution {
            if (!isPending) return resolved(initial)
            val validation = MutableStateFlow(initial)
            val isStatusPending = MutableStateFlow(true)
            val job = scope.launch(Dispatchers.Default) {
                try {
                    catchingUnlessCancelled { resolve() }
                        .onSuccess { validation.value = it }
                        .onFailure { Napier.w("Could not resolve the status of the relying party", it) }
                } finally {
                    isStatusPending.value = false
                }
            }
            return RelyingPartyStatusResolution(validation.asStateFlow(), isStatusPending.asStateFlow(), job)
        }
    }
}

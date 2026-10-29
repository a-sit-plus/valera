package at.asitplus.wallet.app.common.presentation

import at.asitplus.wallet.app.common.catchingUnlessCancelled
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyValidationUi
import at.asitplus.wallet.lib.openid.AuthorizationRequestValidation
import at.asitplus.wallet.lib.openid.AuthorizationRequestValidationPendingStatus
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.multipaz.util.UUID

/**
 * The OpenID4VP requests validated in this session, until their consent screen is left. The route of the consent
 * screen names a request by its id instead of carrying it: a request with its request object, preparation state,
 * and validation report can exceed what Android saves for a back stack entry.
 *
 * Owned by the session, so the requests of a closed session, or of a process that has died, are gone: a consent
 * screen restored from a saved back stack fails, and the request has to be opened again.
 */
class PresentationRequestStore {
    private val requests = MutableStateFlow<Map<String, ValidatedPresentationRequest>>(emptyMap())

    /** Keeps [request] until [remove], returning the id to look it up with. */
    fun put(request: ValidatedPresentationRequest): String = UUID.randomUUID().toString().also { id ->
        requests.update { it + (id to request) }
    }

    operator fun get(id: String): ValidatedPresentationRequest? = requests.value[id]

    /** Drops the request [id], and stops resolving the status of its relying party. */
    fun remove(id: String) {
        var removed: ValidatedPresentationRequest? = null
        requests.update { removed = it[id]; it - id }
        removed?.cancel()
    }
}

/**
 * A validated OpenID4VP request, with the [relyingPartyValidation] to show for it, which is replaced once the status
 * of the registration certificates (WRPRCs) of its relying party is resolved, see [resolvingStatus].
 */
class ValidatedPresentationRequest(
    val preparationState: AuthorizationResponsePreparationState,
    val relyingPartyValidation: StateFlow<RelyingPartyValidationUi?>,
    /** Whether the status of a WRPRC of the relying party is still being retrieved. */
    val isStatusPending: StateFlow<Boolean>,
    private val statusResolution: Job? = null,
) {
    /** Stops resolving the status, e.g. once the consent screen is left. */
    fun cancel() {
        statusResolution?.cancel()
    }

    companion object {
        /**
         * The request of [pending], to show at once, whose WRPRC status is resolved in [scope], replacing its
         * [relyingPartyValidation]. A status that can not be resolved stays as not checked.
         */
        fun resolvingStatus(
            pending: AuthorizationRequestValidationPendingStatus,
            scope: CoroutineScope,
        ): ValidatedPresentationRequest {
            val validation = MutableStateFlow(pending.validation.relyingPartyValidationUi())
            val isStatusPending = MutableStateFlow(pending.isPending)
            val statusResolution = if (pending.isPending) scope.launch(Dispatchers.Default) {
                try {
                    catchingUnlessCancelled { pending.resolveStatus() }
                        .onSuccess { validation.value = it.relyingPartyValidationUi() }
                        .onFailure { Napier.w("Could not resolve the status of the relying party", it) }
                } finally {
                    isStatusPending.value = false
                }
            } else null
            return ValidatedPresentationRequest(
                preparationState = pending.validation.preparationState,
                relyingPartyValidation = validation.asStateFlow(),
                isStatusPending = isStatusPending.asStateFlow(),
                statusResolution = statusResolution,
            )
        }

        private fun AuthorizationRequestValidation.relyingPartyValidationUi() =
            RelyingPartyValidationUi.of(validationReport, relyingParty)
    }
}

package at.asitplus.wallet.app.common.presentation

import at.asitplus.wallet.app.common.relyingParty.RelyingPartyStatusResolution
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyValidationUi
import at.asitplus.wallet.lib.openid.AuthorizationRequestValidation
import at.asitplus.wallet.lib.openid.AuthorizationRequestValidationPendingStatus
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
 * A validated OpenID4VP request, with the validation of its [relyingParty] to show, which is replaced once the status
 * of its registration certificates is resolved, see [resolvingStatus].
 */
class ValidatedPresentationRequest(
    val preparationState: AuthorizationResponsePreparationState,
    val relyingParty: RelyingPartyStatusResolution,
) {
    /** Stops resolving the status, e.g. once the consent screen is left. */
    fun cancel() = relyingParty.cancel()

    companion object {
        /** The request of [pending], to show at once, whose relying party status is resolved in [scope]. */
        fun resolvingStatus(
            pending: AuthorizationRequestValidationPendingStatus,
            scope: CoroutineScope,
        ) = ValidatedPresentationRequest(
            preparationState = pending.validation.preparationState,
            relyingParty = RelyingPartyStatusResolution.start(
                initial = pending.validation.relyingPartyValidationUi(),
                isPending = pending.isPending,
                scope = scope,
            ) { pending.resolveStatus().relyingPartyValidationUi() },
        )

        private fun AuthorizationRequestValidation.relyingPartyValidationUi() =
            RelyingPartyValidationUi.of(validationReport, relyingParty)
    }
}

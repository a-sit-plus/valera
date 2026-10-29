package at.asitplus.wallet.app.common.presentation

import at.asitplus.wallet.app.common.relyingParty.RelyingPartyValidationUi
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
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

    fun remove(id: String) = requests.update { it - id }
}

/** A validated OpenID4VP request, with the [relyingPartyValidation] to show for it. */
data class ValidatedPresentationRequest(
    val preparationState: AuthorizationResponsePreparationState,
    val relyingPartyValidation: RelyingPartyValidationUi?,
)

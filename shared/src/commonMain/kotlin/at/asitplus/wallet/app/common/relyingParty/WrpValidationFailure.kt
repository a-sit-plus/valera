package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.wallet.lib.agent.validation.relyingParty.WrpValidationException
import kotlinx.serialization.Serializable

/**
 * Why no [WrpValidationResult] could be obtained for a request, to be shown instead of assuming that no registration
 * certificate was provided.
 */
@Serializable
data class WrpValidationFailure(
    val reason: Reason,
    /** Technical detail, e.g. why the registration certificate could not be parsed. */
    val detail: String? = null,
) {
    enum class Reason {
        /** The request carries no registration certificate. */
        NOT_PROVIDED,

        /** The request carries a registration certificate that cannot be read. */
        MALFORMED,

        /** Registration certificates of this kind of request cannot be checked. */
        UNSUPPORTED,
    }
}

fun Throwable.toWrpValidationFailure(): WrpValidationFailure = when (this) {
    is WrpValidationException.RegistrationCertificateMissing -> WrpValidationFailure(WrpValidationFailure.Reason.NOT_PROVIDED)
    is WrpValidationException.UnsupportedRequest -> WrpValidationFailure(WrpValidationFailure.Reason.UNSUPPORTED, message)
    else -> WrpValidationFailure(WrpValidationFailure.Reason.MALFORMED, message)
}

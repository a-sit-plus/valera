package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.etsi.relyingParty.WrpLangString
import at.asitplus.etsi.relyingParty.WrpPayload
import at.asitplus.wallet.app.common.validation.ArtifactKind
import at.asitplus.wallet.app.common.validation.CheckState
import at.asitplus.wallet.app.common.validation.FindingUi
import at.asitplus.wallet.app.common.validation.UnregisteredRequestUi
import at.asitplus.wallet.app.common.validation.ValidationOutcomeUi
import at.asitplus.wallet.app.common.validation.flatten
import at.asitplus.wallet.app.common.validation.toUi
import at.asitplus.wallet.lib.validation.RelyingPartyValidationResult
import at.asitplus.wallet.lib.validation.ValidationReport
import kotlinx.serialization.Serializable

/**
 * The validation of the relying party of a presentation request, as shown to the user: the report of the request,
 * with its access certificate (WRPAC) and registration certificates (WRPRC), and what the relying party is registered
 * for.
 */
@Serializable
data class RelyingPartyValidationUi(
    /** `null` if the request has no report, e.g. an ISO mdoc request over the DC API without a WRPRC. */
    val outcome: ValidationOutcomeUi?,
    /** What the registration certificate of the relying party says, whether or not it is valid. */
    val registration: WrpDisplayInfo?,
) {
    private val registrationCertificates
        get() = outcome?.root?.flatten()?.filter { it.kind == ArtifactKind.REGISTRATION_CERTIFICATE }.orEmpty()

    /** Whether the request carries no registration certificate. */
    val registrationMissing: Boolean
        get() = registrationCertificates.none()

    /** Whether every registration certificate of the request has been accepted. */
    val registrationVerified: Boolean
        get() = !registrationMissing && registrationCertificates.all { it.accepted }

    /** Whether the request is not signed, so that its sender could not be authenticated. */
    val unsigned: Boolean
        get() = outcome?.root?.takeIf { it.kind == ArtifactKind.REQUEST_OBJECT }
            ?.checks?.any { it.name == "signature" && it.state == CheckState.NOT_APPLICABLE } == true

    /** The requests of the relying party its registration does not cover, with what of them is registered. */
    val unregisteredRequests: List<UnregisteredRequestUi>
        get() = registrationCertificates
            .flatMap { it.checks.asSequence() }
            .mapNotNull { it.finding as? FindingUi.Unregistered }
            .flatMap { it.requests.asSequence() }
            .toList()

    companion object {
        /**
         * The validation of the relying party of a request with its [report], and the result for its [relyingParty],
         * which carries what its WRPRCs say, see [RelyingPartyValidationResult.untrustedRegistrations].
         */
        fun of(report: ValidationReport?, relyingParty: RelyingPartyValidationResult?) = RelyingPartyValidationUi(
            outcome = report?.toUi(),
            registration = relyingParty?.untrustedRegistrations?.firstOrNull()?.toDisplayInfo(),
        )
    }
}

@Serializable
data class WrpDisplayInfo(
    val name: String?,
    val purpose: List<WrpLangString>?,
    val country: String?,
    val infoUri: String?,
    val supportUri: String?,
)

private fun WrpPayload.toDisplayInfo() = WrpDisplayInfo(
    name = name,
    purpose = purpose,
    country = country,
    infoUri = infoUri,
    supportUri = supportUri,
)

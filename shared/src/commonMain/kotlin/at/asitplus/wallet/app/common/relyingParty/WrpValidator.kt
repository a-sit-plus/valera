package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.wallet.lib.agent.TrustedCertificates
import at.asitplus.wallet.lib.agent.validation.TokenStatusResolver
import at.asitplus.wallet.lib.agent.validation.relyingParty.WrpAuthenticationRequestValidator
import at.asitplus.wallet.lib.agent.validation.relyingParty.accessCertificate.WrpacValidator
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrprcValidator
import at.asitplus.wallet.lib.etsi.LoteProfile
import at.asitplus.wallet.lib.openid.validateWrpAuthenticationRequest

/**
 * Class to verify data sent from a relying party during a presentation request.
 * Utilizes specific validators for registration certificates and access certificates.
 * Reports back with a validation result for the UI.
 **/
class WrpValidator(
    val trustListService: TrustListService,
    val tokenStatusResolver: TokenStatusResolver,
) {
    val accessCertValidator = WrpacValidator
    val registrationCertValidator = WrprcValidator()

    suspend fun validate(requestParametersFrom: RequestParametersFrom<*>): KmmResult<WrpValidationResult?> = catching {
        val validationData = when (requestParametersFrom) {
            // Reader authentication signs over the DC API session transcript, which the plain overload cannot build
            is RequestParametersFrom.IsoMdocDcApi -> requestParametersFrom.validateWrpAuthenticationRequest()
            else -> WrpAuthenticationRequestValidator.invoke(requestParametersFrom)
        }.getOrThrow()
        val accessCertTrustList = trustListService.getTrustList(LoteProfile.WRPAC).getOrThrow()
        val accessCertValidation = accessCertValidator.invoke(validationData,
            TrustedCertificates { accessCertTrustList.toSet() }).getOrThrow()

        val registrationCertValidation =
            registrationCertValidator.invoke(
                identifierResult = accessCertValidation.identifierResult,
                validationData = validationData,
                tokenStatusResolver = tokenStatusResolver,
                certificateTrustAnchors = TrustedCertificates { accessCertTrustList.toSet() },
            ).getOrThrow()

        WrpValidationResult(registrationCertValidation, accessCertValidation)
    }
}

package at.asitplus.wallet.app.common.relyingParty

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
import io.github.aakira.napier.Napier

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

    /**
     * Validates the WRPAC and the WRPRC of [requestParametersFrom]. The WRPRC is also validated when the WRPAC is
     * invalid, its linkage to the WRPAC is invalid then.
     */
    suspend fun validate(requestParametersFrom: RequestParametersFrom<*>): WrpValidationResult {
        val validationData = catching {
            when (requestParametersFrom) {
                // Reader authentication signs over the DC API session transcript, which the plain overload cannot build
                is RequestParametersFrom.IsoMdocDcApi -> requestParametersFrom.validateWrpAuthenticationRequest()
                else -> WrpAuthenticationRequestValidator.invoke(requestParametersFrom)
            }.getOrThrow()
        }
        val accessCertTrustList = trustListService.getTrustList(LoteProfile.WRPAC)
        val trustAnchors = TrustedCertificates { accessCertTrustList.getOrThrow().toSet() }
        val accessCertValidation = validationData.transform {
            accessCertValidator.invoke(validationData = it, certificateTrustAnchors = trustAnchors)
        }.onFailure { Napier.w("WRPAC validation failed", it) }
        val registrationCertValidation = validationData.transform {
            registrationCertValidator.invoke(
                identifierResult = accessCertValidation.getOrNull()?.identifierResult,
                validationData = it,
                tokenStatusResolver = tokenStatusResolver,
                certificateTrustAnchors = trustAnchors,
            )
        }.onFailure { Napier.w("WRPRC validation failed", it) }

        return WrpValidationResult(
            accessCertificate = accessCertValidation,
            registrationCertificate = registrationCertValidation,
        )
    }
}

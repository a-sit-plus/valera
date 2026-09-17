package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.catchingUnwrapped
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.openid.VerifierInfo
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.wallet.lib.agent.validation.StatusListTokenResolver
import at.asitplus.wallet.lib.agent.validation.relyingParty.accessCertificate.WrpacValidator
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrprcValidator
import at.asitplus.wallet.lib.etsi.LoTEServiceType
import io.github.aakira.napier.Napier

/**
 * Class to verify data sent from a relying party during a presentation request.
 * Utilizes specific validators for registration certificates and access certificates.
 * Reports back with a validation result for the UI.
 **/
class WrpValidator(
    val trustListService: TrustListService,
    val statusListTokenResolver: StatusListTokenResolver
) {
    suspend fun validate(requestParametersFrom: RequestParametersFrom<*>) = catchingUnwrapped {

        val validationData = requestParametersFrom.parseRequestValidationData() ?: run {
            Napier.w("Unable to parse validationData from request: $requestParametersFrom", tag = LOG_TAG)
            return@catchingUnwrapped null
        }

        val accessCertTrustList = trustListService.getTrustList(LoTEServiceType.WRPAC)

        accessCertTrustList ?: run {
            Napier.w("Unable to fetch access certificate trust list", tag = LOG_TAG)
            return@catchingUnwrapped null
        }

        val accessCertValidation =
            WrpacValidator.validate(validationData = validationData, accessCertTrustList)

        accessCertValidation ?: run {
            Napier.w("Access certificate validation failed", tag = LOG_TAG)
            return@catchingUnwrapped null
        }

        // Use service type WRPAC (with local cert fallback) until WRPRC endpoints exist
        val registrationCertTrustList = trustListService.getTrustList(LoTEServiceType.WRPAC)

        registrationCertTrustList ?: run {
            Napier.w("Unable to fetch registration certificate trust list", tag = LOG_TAG)
            return@catchingUnwrapped null
        }

        val registrationCertValidation =
            WrprcValidator.validate(
                accessCertValidation = accessCertValidation,
                validationData = validationData,
                statusListTokenResolver = statusListTokenResolver,
                certificateTrustAnchors = registrationCertTrustList
            )

        registrationCertValidation ?: run {
            Napier.w("Registration certificate validation failed", tag = LOG_TAG)
            return@catchingUnwrapped null
        }

        WrpValidationResult(registrationCertValidation, accessCertValidation)
    }

    private companion object {
        const val LOG_TAG = "WrpValidator"
    }
}

package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.OpenIdConstants.VerifierInfo.REGISTRATION_CERT_FORMAT
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.wallet.app.common.extractRelyingPartyCertificateChains
import at.asitplus.wallet.lib.agent.TrustedCertificates
import at.asitplus.wallet.lib.agent.validation.TokenStatusResolver
import at.asitplus.wallet.lib.agent.validation.relyingParty.ReaderAuthenticationVerifier
import at.asitplus.wallet.lib.agent.validation.relyingParty.WrpAccessCertificate
import at.asitplus.wallet.lib.agent.validation.relyingParty.WrpAuthenticationRequestValidator
import at.asitplus.wallet.lib.agent.validation.relyingParty.WrpRequestData
import at.asitplus.wallet.lib.agent.validation.relyingParty.accessCertificate.WrpacValidator
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrprcValidator
import at.asitplus.wallet.lib.etsi.LoteProfile
import at.asitplus.wallet.lib.openid.IsoMdocDcapiResponseBuilder
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
     * Validates the WRPAC and the WRPRC of [requestParametersFrom], each independently of the other one, so that the
     * WRPRC is also validated when the WRPAC is invalid (its linkage to the WRPAC is invalid then).
     * A certificate missing from the request is not validated at all.
     */
    suspend fun validate(requestParametersFrom: RequestParametersFrom<*>): WrpValidationResult {
        val accessCertTrustList = trustListService.getTrustList(LoteProfile.WRPAC)
        val trustAnchors = TrustedCertificates { accessCertTrustList.getOrThrow().toSet() }
        val accessCertValidation = requestParametersFrom.toWrpacRequestData()?.transform {
            accessCertValidator.invoke(validationData = it, certificateTrustAnchors = trustAnchors)
        }?.onFailure { Napier.w("WRPAC validation failed", it) }
        val registrationCertValidation = requestParametersFrom.takeIf { it.hasRegistrationCertificate() }
            ?.toWrpRequestData()
            ?.transform {
                registrationCertValidator.invoke(
                    identifierResult = accessCertValidation?.getOrNull()?.identifierResult,
                    validationData = it,
                    tokenStatusResolver = tokenStatusResolver,
                    certificateTrustAnchors = trustAnchors,
                )
            }?.onFailure { Napier.w("WRPRC validation failed", it) }

        return WrpValidationResult(
            accessCertificate = accessCertValidation,
            registrationCertificate = registrationCertValidation,
        )
    }
}

private suspend fun RequestParametersFrom<*>.toWrpRequestData(): KmmResult<WrpRequestData> = catching {
    when (this) {
        // Reader authentication signs over the DC API session transcript, which the plain overload cannot build
        is RequestParametersFrom.IsoMdocDcApi -> validateWrpAuthenticationRequest()
        else -> WrpAuthenticationRequestValidator.invoke(this)
    }.getOrThrow()
}

/**
 * Data to validate only the WRPAC, which [WrpAuthenticationRequestValidator] can not provide without a parseable
 * WRPRC, or `null` if the request does not contain a WRPAC.
 */
internal suspend fun RequestParametersFrom<*>.toWrpacRequestData(): KmmResult<WrpRequestData>? = when (this) {
    is RequestParametersFrom.IsoMdocDcApi -> {
        val deviceRequest = parameters.isoMdocRequest.deviceRequest
        if (deviceRequest.readerAuthAll.isNullOrEmpty() && deviceRequest.docRequests.all { it.readerAuth == null }) {
            null
        } else catching {
            val transcript = IsoMdocDcapiResponseBuilder.sessionTranscriptFor(this)
            WrpRequestData(
                accessCertificate = WrpAccessCertificate(
                    ReaderAuthenticationVerifier().invoke(deviceRequest, transcript).getOrThrow()
                ),
                registrationCertificate = emptyMap(),
            )
        }
    }

    else -> extractRelyingPartyCertificateChains()?.let { chain ->
        KmmResult.success(
            WrpRequestData(
                clientId = (parameters as? AuthenticationRequestParameters)?.clientId,
                accessCertificate = WrpAccessCertificate(chain),
                registrationCertificate = emptyMap(),
            )
        )
    }
}

internal fun RequestParametersFrom<*>.hasRegistrationCertificate(): Boolean = when (this) {
    is RequestParametersFrom.IsoMdocDcApi -> parameters.isoMdocRequest.deviceRequest.docRequests.any {
        it.itemsRequest.value.requestInfo?.euWrprc != null
    }

    else -> (parameters as? AuthenticationRequestParameters)?.verifierInfo.orEmpty().any {
        it.format.equals(REGISTRATION_CERT_FORMAT, ignoreCase = true)
    }
}

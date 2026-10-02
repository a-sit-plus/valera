package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.catchingUnwrapped
import at.asitplus.openid.AuthenticationRequestParameters
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
import at.asitplus.openid.dcql.DCQLQuery
import at.asitplus.wallet.lib.openid.VerifierSignature
import at.asitplus.wallet.lib.openid.wrpRequestDataOfSigners
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrprcValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpCredentialRequest
import at.asitplus.wallet.lib.agent.validation.relyingParty.MissingRegistrationCertificateException
import at.asitplus.wallet.lib.agent.validation.relyingParty.UnsupportedWrpRequestException
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
     *
     * A multisigned request is validated per signer, and only for the signers that [verifierSignatures], i.e. the
     * result of validating the request, reports as authenticated: `null` until those are known.
     */
    suspend fun validate(
        requestParametersFrom: RequestParametersFrom<*>,
        verifierSignatures: List<VerifierSignature>? = null,
    ): WrpValidationResult? {
        val accessCertTrustList = trustListService.getTrustList(LoteProfile.WRPAC)
        val trustAnchors = TrustedCertificates { accessCertTrustList.getOrThrow().toSet() }
        if (requestParametersFrom is RequestParametersFrom.OpenId4VpDcApiMultiSigned) {
            // Until the request is validated, no signer is known to have signed
            verifierSignatures ?: return null
            return validateMultiSigned(requestParametersFrom, verifierSignatures, trustAnchors)
        }
        val accessCertRequestData = requestParametersFrom.toWrpacRequestData()
        val accessCertValidation = accessCertRequestData?.transform {
            accessCertValidator.invoke(validationData = it, certificateTrustAnchors = trustAnchors)
        }?.onFailure { Napier.w("WRPAC validation failed", it) }
        val registrationCertValidation = requestParametersFrom.toWrprcRequestData()
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
        ).copy(
            clientId = (requestParametersFrom.parameters as? AuthenticationRequestParameters)?.clientId,
            accessCertificateLeaf = accessCertRequestData?.getOrNull()?.accessCertificate?.certificateChain?.firstOrNull(),
            registrationCertificateSigner = registrationCertValidation?.getOrNull()
                ?.certificateValidationResults?.keys?.firstOrNull()?.signerCertificate(),
            uncoveredCredentialQueryIds = uncoveredCredentialQueryIds(
                (requestParametersFrom.parameters as? AuthenticationRequestParameters)?.dcqlQuery,
                listOfNotNull(registrationCertValidation?.getOrNull()),
            )
        )
    }

    /**
     * Like [validate], but a failure, e.g. because the trust list is not available, is a result saying that the
     * relying party could not be checked, as it is no reason to refuse the presentation.
     */
    suspend fun validateOrReportFailure(
        requestParametersFrom: RequestParametersFrom<*>,
        verifierSignatures: List<VerifierSignature>? = null,
    ): WrpValidationResult? = catchingUnwrapped { validate(requestParametersFrom, verifierSignatures) }.getOrElse {
        Napier.w("WRP validation failed", it)
        WrpValidationResult(
            accessCertificate = null,
            registrationCertificate = null,
            registrationCertificateError = it.displayText(),
        )
    }

    /**
     * Validates every authenticated signer of [request] on its own, with the access and registration certificates
     * from its own protected header only, see [wrpRequestDataOfSigners].
     */
    private suspend fun validateMultiSigned(
        request: RequestParametersFrom.OpenId4VpDcApiMultiSigned,
        verifierSignatures: List<VerifierSignature>,
        trustAnchors: TrustedCertificates,
    ): WrpValidationResult {
        val signers = request.wrpRequestDataOfSigners(verifierSignatures).getOrElse {
            Napier.w("WRP validation of multisigned request failed", it)
            return WrpValidationResult(
                accessCertificate = null,
                registrationCertificate = null,
                multiSigned = true,
                signersError = it.displayText(),
            )
        }
        val registrationCertValidations = mutableListOf<WrprcValidationResult>()
        val validations = signers.map { signer ->
            val data = signer.requestData.getOrElse { error ->
                // e.g. a protected header that does not match the signature result, so nothing can be trusted
                Napier.w("WRP data of signature ${signer.signatureIndex} is invalid", error)
                return@map SignerWrpValidation(
                    signatureIndex = signer.signatureIndex,
                    clientId = signer.clientId,
                    identity = WrpValidationResult(
                        accessCertificate = null,
                        accessCertificateError = error.displayText(),
                        registrationCertificate = null,
                        registrationCertificateError = error.displayText(),
                    ),
                )
            }
            val accessCertValidation = data.takeIf { it.accessCertificate.certificateChain != null }?.let {
                accessCertValidator.invoke(validationData = it, certificateTrustAnchors = trustAnchors)
                    .onFailure { Napier.w("WRPAC of signature ${signer.signatureIndex} is invalid", it) }
            }
            val registrationCertValidation = data.takeIf { it.registrationCertificate.isNotEmpty() }?.let {
                registrationCertValidator.invoke(
                    identifierResult = accessCertValidation?.getOrNull()?.identifierResult,
                    validationData = it,
                    tokenStatusResolver = tokenStatusResolver,
                    certificateTrustAnchors = trustAnchors,
                ).onFailure { Napier.w("WRPRC of signature ${signer.signatureIndex} is invalid", it) }
            }
            registrationCertValidation?.getOrNull()?.let { registrationCertValidations += it }
            SignerWrpValidation(
                signatureIndex = signer.signatureIndex,
                clientId = signer.clientId,
                identity = WrpValidationResult(
                    accessCertificate = accessCertValidation,
                    registrationCertificate = registrationCertValidation,
                ).copy(
                    clientId = signer.clientId,
                    accessCertificateLeaf = data.accessCertificate.certificateChain?.firstOrNull(),
                    registrationCertificateSigner = data.registrationCertificate.keys.firstOrNull()?.signerCertificate(),
                ),
            )
        }
        return WrpValidationResult(
            accessCertificate = null,
            registrationCertificate = null,
            multiSigned = true,
            signers = validations,
            uncoveredCredentialQueryIds = uncoveredCredentialQueryIds(
                request.parameters.dcqlQuery,
                registrationCertValidations,
            ),
        )
    }
}

/**
 * Credential queries of [dcqlQuery] that none of [registrationCertificates] applies to, see `credential_ids`, or none
 * if no registration certificate was validated.
 */
internal fun uncoveredCredentialQueryIds(
    dcqlQuery: DCQLQuery?,
    registrationCertificates: List<WrprcValidationResult>,
): List<String> {
    if (dcqlQuery == null || registrationCertificates.isEmpty()) return emptyList()
    val covered = registrationCertificates.flatMap { result ->
        result.requestDataValidationResults.mapNotNull { (request, _) ->
            (request as? WrpCredentialRequest.WrpDcqlCredentialQuery)?.query?.id?.string
        }
    }.toSet()
    return dcqlQuery.credentials.map { it.id.string }.filterNot { it in covered }
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

    else -> catching { extractRelyingPartyCertificateChains() }.fold(
        onSuccess = { chain ->
            chain?.let {
                KmmResult.success(
                    WrpRequestData(
                        clientId = (parameters as? AuthenticationRequestParameters)?.clientId,
                        accessCertificate = WrpAccessCertificate(it),
                        registrationCertificate = emptyMap(),
                    )
                )
            }
        },
        onFailure = { KmmResult.failure(it) },
    )
}

/**
 * Data to validate the WRPRC, or `null` if the request does not contain one ([MissingRegistrationCertificateException]).
 * Requests that can not be validated for a relying party at all ([UnsupportedWrpRequestException]), e.g. unsigned
 * ones, count as not containing a WRPRC either.
 */
internal suspend fun RequestParametersFrom<*>.toWrprcRequestData(): KmmResult<WrpRequestData>? =
    toWrpRequestData().takeUnless {
        val error = it.exceptionOrNull()
        error is MissingRegistrationCertificateException || error is UnsupportedWrpRequestException
    }

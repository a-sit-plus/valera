package at.asitplus.wallet.app.common.relyingParty.validation

import at.asitplus.catchingUnwrapped
import at.asitplus.data.NonEmptyList
import at.asitplus.data.NonEmptyList.Companion.toNonEmptyList
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.openid.VerifierInfo
import at.asitplus.signum.indispensable.josef.JwsCompact
import at.asitplus.signum.indispensable.josef.JwsTyped
import at.asitplus.signum.indispensable.pki.CertificateChain
import at.asitplus.wallet.app.common.relyingParty.data.WrpLangStringDto
import at.asitplus.wallet.app.common.relyingParty.validation.certificates.accessCertificate.WrpacValidator
import at.asitplus.wallet.app.common.relyingParty.validation.certificates.registrationCertificate.RequestDataValidity
import at.asitplus.wallet.app.common.relyingParty.validation.certificates.registrationCertificate.WrprcValidator
import at.asitplus.wallet.app.common.relyingParty.validation.certificates.registrationCertificate.getDisplayInfo
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable

/**
 * Class to verify data sent from a relying party during a presentation request.
 * Utilizes specific validators for registration certificates and access certificates.
 * Reports back with a validation result for the UI.
 **/
class WrpValidator(
    val wrpacValidator: WrpacValidator,
    val wrprcValidator: WrprcValidator,
) {
    suspend fun validate(requestParametersFrom: RequestParametersFrom<*>) = catchingUnwrapped {
        val request = requestParametersFrom as? RequestParametersFrom<AuthenticationRequestParameters> ?: run {
            Napier.w(
                "Can't cast request as RequestParametersFrom<AuthenticationRequestParameters>: $requestParametersFrom",
                tag = LOG_TAG
            )
            return@catchingUnwrapped null
        }

        val validationData = request.parseRequestValidationData() ?: run {
            Napier.w("Unable to parse validationData from request: $request", tag = LOG_TAG)
            return@catchingUnwrapped null
        }

        validationData.verifierInfo ?: run {
            Napier.w("VerifierInfo is missing in request $request", tag = LOG_TAG)
            return@catchingUnwrapped null
        }
        val accessCertValidation = wrpacValidator.validate(requestParametersFrom)

        accessCertValidation ?: run {
            Napier.w("Access certificate chain validation failed", tag = LOG_TAG)
            return@catchingUnwrapped null
        }

        val registrationCertValidation =
            wrprcValidator.validateVerifierInfoList(validationData.verifierInfo, accessCertValidation.identifierResult)

        val requestDataValidity = wrprcValidator.validateRequest(validationData, registrationCertValidation)

        val displayInfo = registrationCertValidation.getDisplayInfo()

        val wrprcValid =
            registrationCertValidation.isNotEmpty() && registrationCertValidation.all { it.value?.isValid() == true }

        WrpValidationResult(
            displayInfo = displayInfo,
            requestDataInfo = requestDataValidity,
            wrpacValid = accessCertValidation.hashValid && accessCertValidation.chainValid,
            wrprcValid = wrprcValid
        )
    }

    private fun RequestParametersFrom<AuthenticationRequestParameters>.parseRequestValidationData() =
        catchingUnwrapped {
            when (this) {
                is RequestParametersFrom.Jws<*> -> {
                    (this.jwsTyped as? JwsTyped<JwsCompact, AuthenticationRequestParameters>)?.let { request ->
                        WrpRequestValidationData(
                            clientId = request.payload.clientId,
                            certificateChain = request.jws.jwsHeader.certificateChain,
                            verifierInfo = this.parameters.verifierInfo?.toNonEmptyList(),
                            request = CredentialPresentationRequest.DCQLRequest(
                                this.parameters.dcqlQuery ?: return@catchingUnwrapped null
                            ),
                        )
                    } ?: run {
                        Napier.w("Unable to cast request as JwsTyped<JwsCompact, AuthenticationRequestParameters>")
                        return@catchingUnwrapped null
                    }
                }

                is RequestParametersFrom.OpenId4VpDcApiSigned -> {
                    WrpRequestValidationData(
                        clientId = this.parameters.clientId,
                        certificateChain = this.jwsTyped.jws.jwsHeader.certificateChain,
                        verifierInfo = this.parameters.verifierInfo?.toNonEmptyList(),
                        request = CredentialPresentationRequest.DCQLRequest(
                            this.parameters.dcqlQuery ?: return@catchingUnwrapped null
                        ),
                    )
                }

                is RequestParametersFrom.IsoMdocDcApi -> {
                    TODO()
                }

                else -> {
                    Napier.w("Request not supported for validation $this")
                    return@catchingUnwrapped null
                }
            }
        }.getOrNull()

    private companion object {
        const val LOG_TAG = "RequestCertificateValidator[WRPAC/WRPRC]"
    }
}

data class WrpRequestValidationData(
    val clientId: String? = null,
    val certificateChain: CertificateChain? = null,
    val verifierInfo: NonEmptyList<VerifierInfo>? = null,
    val request: CredentialPresentationRequest? = null,
)

@Serializable
data class WrpDisplayInfo(
    val name: String?,
    val purpose: List<WrpLangStringDto>?,
    val country: String?,
    val infoUri: String?,
    val supportUri: String?,
)

@Serializable
data class WrpValidationResult(
    val displayInfo: WrpDisplayInfo? = null,
    val requestDataInfo: Map<String, RequestDataValidity?>? = null,
    val wrpacValid: Boolean? = null,
    val wrprcValid: Boolean? = null,
) {
    val validAttributes =
        this.requestDataInfo?.all { it.value?.credentialAttributesValidity?.all { it.second } == true } == true
    val validCredentialType = this.requestDataInfo?.all { it.value?.credentialTypeValidity == true } == true
}
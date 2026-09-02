package at.asitplus.wallet.app.common.relyingParty

import androidx.compose.ui.text.intl.Locale
import at.asitplus.catchingUnwrapped
import at.asitplus.data.NonEmptyList.Companion.toNonEmptyList
import at.asitplus.etsi.relyingParty.WrpLangString
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.josef.JwsCompact
import at.asitplus.signum.indispensable.josef.JwsTyped
import at.asitplus.wallet.app.common.extractCertificateChain
import at.asitplus.wallet.lib.agent.validation.relyingParty.WrpRequestValidationData
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import io.github.aakira.napier.Napier

fun List<WrpLangString>.getCurrentLocalization(): String? =
    this.firstOrNull {
        it.lang.lowercase().contains(Locale.current.language.lowercase())
    }?.value ?: this.firstOrNull()?.value

fun RequestParametersFrom<*>.parseRequestValidationData() =
    catchingUnwrapped {
        when (this) {
            is RequestParametersFrom.Jws<*> -> {
                (this.jwsTyped as? JwsTyped<JwsCompact, AuthenticationRequestParameters>)?.let { request ->
                    WrpRequestValidationData(
                        clientId = request.payload.clientId,
                        certificateChain = request.jws.jwsHeader.certificateChain,
                        verifierInfo = request.payload.verifierInfo?.toNonEmptyList(),
                        request = CredentialPresentationRequest.DCQLRequest(
                            request.payload.dcqlQuery ?: return@catchingUnwrapped null
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
                val certificateChain = this.parameters.isoMdocRequest.deviceRequest.extractCertificateChain()
                val request =
                    CredentialPresentationRequest.IsoDeviceRetrieval(this.parameters.isoMdocRequest.deviceRequest)
                WrpRequestValidationData(
                    certificateChain = certificateChain,
                    request = request
                )
                return@catchingUnwrapped null
            }

            else -> {
                Napier.w("Request not supported for validation $this")
                return@catchingUnwrapped null
            }
        }
    }.getOrNull()

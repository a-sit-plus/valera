package at.asitplus.wallet.app.common.domain.requestcertificates

import at.asitplus.iso.sha256
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.io.Base64UrlStrict
import at.asitplus.signum.indispensable.josef.JwsCompact
import at.asitplus.signum.indispensable.josef.JwsCompactTyped
import at.asitplus.signum.indispensable.josef.JwsTyped
import at.asitplus.signum.indispensable.pki.CertificateChain
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import io.github.aakira.napier.Napier
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString

internal data class WrpacRequestValidationResult(
    val isValid: Boolean,
    val wrpIdentifier: String? = null,
    val chainValidated: Boolean = false,
    val clientIdX509HashMatched: Boolean? = null
)

internal class WrpacRequestValidator(
    private val chainValidator: WrpacCertificateChainValidator = WrpacCertificateChainValidator()
) {
    private val tag = "[WRPAC] WrpacRequestValidator"

    fun validate(preparationState: AuthorizationResponsePreparationState): WrpacRequestValidationResult {
        val params = preparationState.request.parameters
        return when (val request = preparationState.request) {
            is RequestParametersFrom.OpenId4VpDcApiSigned -> {
                Napier.d(
                    "validating mandatory request x5c, count=${request.jwsTyped.jws.jwsHeader.certificateChain?.size ?: 0}",
                    tag = tag
                )
                val clientIdHashMatched = validateX509HashBinding(params.clientId, request.jwsTyped.jws.jwsHeader.certificateChain)
                val chainValid = chainValidator.validateMandatoryChain(
                    request.jwsTyped.jws.jwsHeader.certificateChain,
                    source = "WRPAC request x5c"
                ) != null
                reportResult(chainValid = chainValid, clientIdHashMatched = clientIdHashMatched)
            }

            is RequestParametersFrom.Jws<*> -> {
                val request = (request.jwsTyped as JwsTyped<JwsCompact, AuthenticationRequestParameters>)
                Napier.d(
                    "validating optional request x5c, count=${request.jws.jwsHeader.certificateChain?.size ?: 0}",
                    tag = tag
                )
                val chain = request.jws.jwsHeader.certificateChain
                if (chain.isNullOrEmpty()) {
                    Napier.d("optional request x5c missing; skipping WRPAC certificate validation.", tag = tag)
                    WrpacRequestValidationResult(isValid = true)
                } else {
                    val clientIdHashMatched = validateX509HashBinding(params.clientId, chain)
                    val chainValid = chainValidator.validateOptionalChain(chain, source = "WRPAC request x5c") != null
                    reportResult(chainValid = chainValid, clientIdHashMatched = clientIdHashMatched)
                }
            }

            else -> WrpacRequestValidationResult(isValid = true)
        }
    }

    private fun reportResult(chainValid: Boolean, clientIdHashMatched: Boolean?): WrpacRequestValidationResult {
        val isValid = chainValid && clientIdHashMatched != false
        if (isValid) {
            Napier.i("WRPAC request validation passed", tag = tag)
        } else {
            Napier.e("WRPAC request validation failed", tag = tag)
        }
        return WrpacRequestValidationResult(
            isValid = isValid,
            chainValidated = chainValid,
            clientIdX509HashMatched = clientIdHashMatched
        )
    }

    private fun validateX509HashBinding(clientId: String?, chain: CertificateChain?): Boolean? {
        if (chain.isNullOrEmpty()) {
            Napier.d("x509_hash validation skipped, request x5c missing.", tag = tag)
            return null
        }
        if (clientId.isNullOrBlank()) {
            Napier.d("x509_hash validation skipped, client_id missing.", tag = tag)
            return null
        }
        if (!clientId.startsWith("x509_hash:")) {
            Napier.d("x509_hash validation skipped, client_id is '$clientId'.", tag = tag)
            return null
        }

        val expectedHash = clientId.removePrefix("x509_hash:")
        val calculatedHash = runCatching {
            chain.first().encodeToDer().sha256().encodeToString(Base64UrlStrict)
        }.getOrElse {
            Napier.e("x509_hash calculation from request x5c[0] failed.", it, tag = tag)
            return false
        }

        Napier.d("x509_hash expected(client_id)=$expectedHash", tag = tag)
        Napier.d("x509_hash calculated(request x5c[0])=$calculatedHash", tag = tag)
        return if (calculatedHash == expectedHash) {
            Napier.i("x509_hash binding passed.", tag = tag)
            true
        } else {
            Napier.e("x509_hash binding failed.", tag = tag)
            false
        }
    }
}

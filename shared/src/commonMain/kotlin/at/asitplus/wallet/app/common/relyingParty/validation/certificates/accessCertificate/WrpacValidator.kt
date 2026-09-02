package at.asitplus.wallet.app.common.relyingParty.validation.certificates.accessCertificate

import at.asitplus.catchingUnwrapped
import at.asitplus.iso.sha256
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.asn1.ObjectIdentifier
import at.asitplus.signum.indispensable.io.Base64UrlStrict
import at.asitplus.signum.indispensable.josef.JwsCompact
import at.asitplus.signum.indispensable.josef.JwsTyped
import at.asitplus.signum.indispensable.pki.CertificateChain
import at.asitplus.signum.indispensable.pki.leaf
import at.asitplus.wallet.app.common.relyingParty.validation.certificates.WrpChainValidator
import io.github.aakira.napier.Napier
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString

/**
 * Class to verify access certificates validity.
 **/
class WrpacValidator(
    private val chainValidator: WrpChainValidator
) {
    fun validate(request: RequestParametersFrom<AuthenticationRequestParameters>) = catchingUnwrapped {
        val (clientId, certificateChain) = when (request) {
            is RequestParametersFrom.OpenId4VpDcApiSigned -> {
                request.parameters.clientId to request.jwsTyped.jws.jwsHeader.certificateChain
            }

            is RequestParametersFrom.Jws<*> -> {
                (request.jwsTyped as JwsTyped<JwsCompact, AuthenticationRequestParameters>).let { request ->
                    request.payload.clientId to request.jws.jwsHeader.certificateChain
                }
            }

            else -> {
                Napier.w("Request unsupported for chain validation: $request")
                return@catchingUnwrapped null
            }
        }

        if (certificateChain == null) {
            Napier.w("Certificate chain null")
            return@catchingUnwrapped null
        }

        Napier.d(
            "validating request x5c, count=${certificateChain.size}", tag = LOG_TAG
        )
        val clientIdHashMatched = validateX509HashBinding(
            clientId, certificateChain
        )
        val chainValid = chainValidator.validateChain(
            certificateChain, source = "WRPAC request x5c"
        )

        if (chainValid && clientIdHashMatched) {
            Napier.i("WRPAC request validation passed", tag = LOG_TAG)
        } else {
            Napier.w("WRPAC request validation failed", tag = LOG_TAG)
        }
        val identifierResult = certificateChain.leaf.getWrpIdentifier()

        WrpacValidationResult(
            chain = certificateChain,
            chainValid = chainValid,
            hashValid = clientIdHashMatched,
            identifierResult = identifierResult
        )
    }.getOrElse { error ->
        Napier.w("Unable to validate access certificate!", throwable = error, tag = LOG_TAG)
        null
    }

    private fun validateX509HashBinding(clientId: String?, chain: CertificateChain?): Boolean {
        if (chain.isNullOrEmpty()) {
            Napier.d("x509_hash validation skipped, request x5c missing.", tag = LOG_TAG)
            return false
        }
        if (clientId.isNullOrBlank()) {
            Napier.d("x509_hash validation skipped, client_id missing.", tag = LOG_TAG)
            return false
        }
        if (!clientId.startsWith("x509_hash:")) {
            Napier.d("x509_hash validation skipped, client_id is '$clientId'.", tag = LOG_TAG)
            return false
        }

        val expectedHash = clientId.removePrefix("x509_hash:")
        val calculatedHash = catchingUnwrapped {
            chain.first().encodeToDer().sha256().encodeToString(Base64UrlStrict)
        }.getOrElse {
            Napier.e("x509_hash calculation from request x5c[0] failed.", it, tag = LOG_TAG)
            return false
        }

        Napier.d("x509_hash expected(client_id)=$expectedHash", tag = LOG_TAG)
        Napier.d("x509_hash calculated(request x5c[0])=$calculatedHash", tag = LOG_TAG)
        return if (calculatedHash == expectedHash) {
            Napier.i("x509_hash binding passed.", tag = LOG_TAG)
            true
        } else {
            Napier.e("x509_hash binding failed.", tag = LOG_TAG)
            false
        }
    }

    companion object Constants {
        const val LOG_TAG = "[WRPAC] WrpacRequestValidator"
        val OID_ORGANIZATION_IDENTIFIER = ObjectIdentifier("2.5.4.97")
        val OID_SERIAL_NUMBER = ObjectIdentifier("2.5.4.5")
    }
}
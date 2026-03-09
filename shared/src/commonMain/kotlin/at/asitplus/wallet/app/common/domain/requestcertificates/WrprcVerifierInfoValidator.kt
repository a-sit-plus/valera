package at.asitplus.wallet.app.common.domain.requestcertificates

import at.asitplus.signum.indispensable.josef.JwsAlgorithm
import at.asitplus.signum.indispensable.josef.JwsCompactTyped
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.lib.jws.VerifyJwsSignature
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.JsonObject

internal class WrprcVerifierInfoValidator(
    private val chainValidator: WrpacCertificateChainValidator,
) {
    private val tag = "[WRPRC] WrprcVerifierInfoValidator"

    suspend fun validateAndExtractPayloads(entries: List<ParsedWrprcVerifierInfo>): List<JsonObject> {
        val payloads = mutableListOf<JsonObject>()
        Napier.d("validating entries=${entries.size}", tag = tag)

        entries.forEach { entry ->
            Napier.d(
                "entry[${entry.index}] start, " +
                        "typ='${entry.jwsTyped.jws.jwsHeader.type}', alg='${entry.jwsTyped.jws.jwsHeader.algorithm.identifier}', " +
                        "x5cCount=${entry.jwsTyped.jws.jwsHeader.certificateChain?.size ?: 0}",
                tag = tag
            )
            if (!validateHeader(entry.index, entry.jwsTyped)) return@forEach
            val leafCertificate = chainValidator.validateMandatoryChain(
                entry.jwsTyped.jws.jwsHeader.certificateChain,
                source = "WRPRC verifier_info[${entry.index}] (registration_cert) x5c"
            ) ?: return@forEach
            if (!validateSignature(entry.index, entry.jwsTyped, leafCertificate)) return@forEach
            if (!validatePayload(entry.index, entry.jwsTyped.payload)) return@forEach

            Napier.d(
                "entry[${entry.index}] accepted, payloadSummary=" +
                        buildPayloadSummary(entry.jwsTyped.payload),
                tag = tag
            )
            payloads += entry.jwsTyped.payload
        }

        Napier.d("accepted payloads=${payloads.size}", tag = tag)
        return payloads
    }

    private fun validateHeader(index: Int, jwsTyped: JwsCompactTyped<JsonObject>): Boolean {
        if (jwsTyped.jws.jwsHeader.type != "rc-wrp+jwt") {
            Napier.e(
                "verifier_info[$index] (registration_cert) has invalid typ in JWS header. " +
                        "expected='rc-wrp+jwt', actual='${jwsTyped.jws.jwsHeader.type}'",
                tag = tag
            )
            return false
        }
        if (jwsTyped.jws.jwsHeader.algorithm.identifier != "ES256") {
            Napier.e(
                "verifier_info[$index] (registration_cert) has invalid alg in JWS header. " +
                        "expected='ES256', actual='${jwsTyped.jws.jwsHeader.algorithm.identifier}'",
                tag = tag
            )
            return false
        }
        Napier.d("header checks passed for entry[$index].", tag = tag)
        return true
    }

    private suspend fun validateSignature(
        index: Int,
        jwsTyped: JwsCompactTyped<JsonObject>,
        leafCertificate: X509Certificate
    ): Boolean {
        val jwsAlgorithm = jwsTyped.jws.jwsHeader.algorithm
        if (jwsAlgorithm !is JwsAlgorithm.Signature) {
            Napier.e("verifier_info[$index] (registration_cert) uses unsupported JWS algorithm.", tag = tag)
            return false
        }

        return runCatching {
            VerifyJwsSignature().invoke(jwsTyped.jws, leafCertificate.decodedPublicKey.getOrThrow()).getOrThrow()
            Napier.d("signature validation passed for entry[$index].", tag = tag)
            true
        }.getOrElse {
            Napier.e("verifier_info[$index] (registration_cert) signature validation failed.", it, tag = tag)
            false
        }
    }

    private fun validatePayload(index: Int, payload: JsonObject): Boolean {
        val requiredStringClaims = listOf(
            WrprcPayloadClaims.Subject,
            WrprcPayloadClaims.Name,
            WrprcPayloadClaims.RegistryUri,
            WrprcPayloadClaims.IntendedUseId,
            WrprcPayloadClaims.PrivacyPolicy
        )
        requiredStringClaims.forEach { claim ->
            if (payload.stringField(claim) == null) {
                Napier.e(
                    "verifier_info[$index] (registration_cert) is missing required payload claim '$claim'.",
                    tag = tag
                )
                return false
            }
        }
        if (!payload.hasNonEmptyArray(WrprcPayloadClaims.ServiceDescription)) {
            Napier.e(
                "verifier_info[$index] (registration_cert) is missing required payload claim " +
                    "'${WrprcPayloadClaims.ServiceDescription}'.",
                tag = tag
            )
            return false
        }
        if (!payload.hasNonEmptyArray(WrprcPayloadClaims.Credentials)) {
            Napier.e(
                "verifier_info[$index] (registration_cert) is missing required payload claim " +
                    "'${WrprcPayloadClaims.Credentials}'.",
                tag = tag
            )
            return false
        }
        val issuedAt = payload.longField(WrprcPayloadClaims.IssuedAt)
        if (issuedAt == null) {
            Napier.e(
                "verifier_info[$index] (registration_cert) is missing required payload claim " +
                    "'${WrprcPayloadClaims.IssuedAt}'.",
                tag = tag
            )
            return false
        }
        val expiration = payload.longField(WrprcPayloadClaims.Expiration)
        if (expiration == null) {
            Napier.e(
                "verifier_info[$index] (registration_cert) is missing required payload claim " +
                    "'${WrprcPayloadClaims.Expiration}'.",
                tag = tag
            )
            return false
        }
        if (expiration <= issuedAt) {
            Napier.e(
                "verifier_info[$index] (registration_cert) has invalid temporal claims: exp=$expiration <= iat=$issuedAt.",
                tag = tag
            )
            return false
        }
        Napier.d("payload checks passed for entry[$index].", tag = tag)
        return true
    }

    private fun buildPayloadSummary(payload: JsonObject): String {
        val name = payload.stringField(WrprcPayloadClaims.Name)
        val issuer = payload.stringField(WrprcPayloadClaims.Issuer)
        val subjectId = payload.stringField(WrprcPayloadClaims.Subject)
        val registryUri = payload.stringField(WrprcPayloadClaims.RegistryUri)
        val intendedUseId = payload.stringField(WrprcPayloadClaims.IntendedUseId)
        return "name=$name, iss=$issuer, sub=$subjectId, registry_uri=$registryUri, intended_use_id=$intendedUseId"
    }
}

package at.asitplus.wallet.app.common.domain.requestcertificates

import at.asitplus.catching
import at.asitplus.openid.VerifierInfo
import at.asitplus.signum.indispensable.josef.JwsCompactTyped
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.JsonObject

internal data class ParsedWrprcVerifierInfo(
    val index: Int,
    val jwsTyped: JwsCompactTyped<JsonObject>,
)

internal class WrprcVerifierInfoParser {
    private val tag = "WrprcVerifierInfoParser[WRPRC]"

    fun parse(verifierInfo: List<VerifierInfo>?): List<ParsedWrprcVerifierInfo> {
        val parsed = mutableListOf<ParsedWrprcVerifierInfo>()
        val entries = verifierInfo.orEmpty()
        Napier.d("verifier_info entries=${entries.size}", tag = tag)

        entries.forEachIndexed { index, entry ->
            Napier.d(
                "entry[$index] format='${entry.format}', " +
                        "dataLength=${entry.data.length}, credentialIds=${entry.credentialIds.size}",
                tag = tag
            )
            if (!entry.format.equals(REGISTRATION_CERT_FORMAT, ignoreCase = true)) {
                Napier.w(
                    "skipping verifier_info[$index], " +
                            "expected '$REGISTRATION_CERT_FORMAT' but got '${entry.format}'.",
                    tag = tag
                )
                return@forEachIndexed
            }
            val jwsTyped = catching {
                JwsCompactTyped<JsonObject>(entry.data)
            }.getOrElse {
                Napier.e("verifier_info[$index] ($REGISTRATION_CERT_FORMAT) contains invalid JWS data.", it, tag = tag)
                return@forEachIndexed
            }

            Napier.d(
                "parsed verifier_info[$index] JWS, " +
                        "typ='${jwsTyped.jws.jwsHeader.type}', alg='${jwsTyped.jws.jwsHeader.algorithm.identifier}', " +
                        "x5cCount=${jwsTyped.jws.jwsHeader.certificateChain?.size ?: 0}",
                tag = tag
            )
            parsed += ParsedWrprcVerifierInfo(index = index, jwsTyped = jwsTyped)
        }

        Napier.d("parsed registration_cert entries=${parsed.size}", tag = tag)
        return parsed
    }

    private companion object {
        const val REGISTRATION_CERT_FORMAT = "registration_cert"
    }
}

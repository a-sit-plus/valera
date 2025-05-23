package at.asitplus.wallet.app.common.domain.vck

import at.asitplus.signum.indispensable.josef.JsonWebKey
import at.asitplus.signum.indispensable.josef.JwsSigned
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.jvm.JvmInline

@JvmInline
value class PublicKeyResolver(
    val jsonWebKeySetResolver: JsonWebKeySetResolver
) {
    suspend operator fun invoke(jws: JwsSigned<*>): Set<JsonWebKey>? =
        if (jws.payloadIssuer() == "https://dss.aegean.gr/rfc-issuer" && jws.header.keyId != null) {
            jsonWebKeySetResolver("https://dss.aegean.gr/.well-known/")
                ?.keys?.firstOrNull { it.keyId == jws.header.keyId }?.let { setOf(it) }
        } else setOf()

    private fun JwsSigned<*>.payloadIssuer() =
        ((payload as? JsonObject?)?.get("iss") as? JsonPrimitive?)?.content
}
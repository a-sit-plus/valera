package at.asitplus.wallet.app.common.domain.vck

import at.asitplus.openid.JwtVcIssuerMetadata
import at.asitplus.openid.OAuth2AuthorizationServerMetadata
import at.asitplus.openid.OpenIdConstants.WellKnownPaths
import at.asitplus.signum.indispensable.josef.JsonWebKey
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.lib.oauth2.OAuth2Utils.insertWellKnownPath
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.jvm.JvmInline

class PublicKeyResolver(
    val jsonWebKeySetResolver: JsonWebKeySetResolver,
    val httpService: HttpService
) {
    suspend operator fun invoke(jws: JwsSigned<*>): Set<JsonWebKey>? =
        if (jws.payloadIssuer() == "https://dss.aegean.gr/rfc-issuer" && jws.header.keyId != null) {
            jsonWebKeySetResolver("https://dss.aegean.gr/.well-known/")
                ?.keys?.firstOrNull { it.keyId == jws.header.keyId }?.let { setOf(it) }
        } else if (jws.payloadIssuer() != null) {
            httpService.buildHttpClient()
                .get(insertWellKnownPath(jws.payloadIssuer()!!, WellKnownPaths.JwtVcIssuer))
                .body<JwtVcIssuerMetadata?>()?.let {
                    it.jsonWebKeySetUrl?.let { jsonWebKeySetResolver(it)?.keys?.toSet() }
                        ?: it.jsonWebKeySet?.keys?.toSet()
                }
        } else setOf()

    private fun JwsSigned<*>.payloadIssuer() =
        ((payload as? JsonObject?)?.get("iss") as? JsonPrimitive?)?.content
}
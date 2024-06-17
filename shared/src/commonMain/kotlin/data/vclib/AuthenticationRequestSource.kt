package data.vclib

import at.asitplus.KmmResult
import io.ktor.http.Url
import kotlinx.serialization.Serializable


@Serializable
sealed class AuthenticationRequestSource {
    @Serializable
    data class JwsSigned(
        val jwsSignedSerialized: String,
    ) : AuthenticationRequestSource() {
        val jwsSigned: KmmResult<at.asitplus.crypto.datatypes.jws.JwsSigned>
            get() = at.asitplus.crypto.datatypes.jws.JwsSigned.parse(jwsSignedSerialized)
    }

    @Serializable
    data class Uri(
        val urlSerialized: String,
    ) : AuthenticationRequestSource() {
        val url: Url
            get() = Url(urlSerialized)
    }

    @Serializable
    data class Json(
        val jsonString: String,
    ) : AuthenticationRequestSource()
}
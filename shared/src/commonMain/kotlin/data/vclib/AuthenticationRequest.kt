package data.vclib

import at.asitplus.KmmResult.Companion.wrap
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParametersFrom
import at.asitplus.wallet.lib.oidc.jsonSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class AuthenticationRequest(
    val source: AuthenticationRequestSource,
    val parameters: AuthenticationRequestParameters,
) {

    fun serialize() = jsonSerializer.encodeToString(this)

    fun toAuthenticationRequestParametersFrom(): AuthenticationRequestParametersFrom {
        return when(source) {
            is AuthenticationRequestSource.Json -> AuthenticationRequestParametersFrom.Json(
                jsonString = source.jsonString,
                parameters = parameters,
            )

            is AuthenticationRequestSource.JwsSigned -> AuthenticationRequestParametersFrom.JwsSigned(
                jwsSigned = source.jwsSigned.getOrThrow(),
                parameters = parameters,
            )

            is AuthenticationRequestSource.Uri -> AuthenticationRequestParametersFrom.Uri(
                url = source.url,
                parameters = parameters,
            )
        }
    }

    companion object {
        fun deserialize(it: String) = kotlin.runCatching {
            jsonSerializer.decodeFromString<AuthenticationRequest>(it)
        }.wrap()

        fun createInstance(parametersFrom: AuthenticationRequestParametersFrom): AuthenticationRequest {
            return AuthenticationRequest(
                source = when (parametersFrom) {
                    is AuthenticationRequestParametersFrom.JwsSigned -> AuthenticationRequestSource.JwsSigned(
                        jwsSignedSerialized = parametersFrom.jwsSigned.serialize()
                    )

                    is AuthenticationRequestParametersFrom.Json -> AuthenticationRequestSource.Json(
                        jsonString = parametersFrom.jsonString
                    )

                    is AuthenticationRequestParametersFrom.Uri -> AuthenticationRequestSource.Uri(
                        urlSerialized = parametersFrom.url.toString()
                    )
                },
                parameters = parametersFrom.parameters
            )
        }
    }
}
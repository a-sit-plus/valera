package data.vclib

import at.asitplus.KmmResult.Companion.wrap
import at.asitplus.wallet.lib.oidc.AuthenticationResponseParameters
import at.asitplus.wallet.lib.oidc.AuthenticationResponseResult
import at.asitplus.wallet.lib.oidc.jsonSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString


/**
 * Possible outcomes of creating the OIDC Authentication Response
 */
@Serializable
sealed class AuthenticationResponseResultSerializable {

    abstract fun toAuthenticationResponseResult(): AuthenticationResponseResult

    /**
     * Wallet returns the [AuthenticationResponseParameters] as form parameters, which shall be posted to
     * `redirect_uri` of the Relying Party, i.e. clients should execute that POST with [params] to [url].
     */
    @Serializable
    data class Post(val url: String, val params: Map<String, String>) :
        AuthenticationResponseResultSerializable() {
        override fun toAuthenticationResponseResult(): AuthenticationResponseResult.Post {
            return AuthenticationResponseResult.Post(
                url = url,
                params = params,
            )
        }
    }

    /**
     * Wallet returns the [AuthenticationResponseParameters] as fragment parameters appended to the
     * `redirect_uri` of the Relying Party, i.e. clients should simply open the [url]. The [params] are also included
     * for further use.
     */
    @Serializable
    data class Redirect(
        val url: String,
        val params: AuthenticationResponseParameters,
    ) : AuthenticationResponseResultSerializable() {
        override fun toAuthenticationResponseResult(): AuthenticationResponseResult.Redirect {
            return AuthenticationResponseResult.Redirect(
                url = url,
                params = params,
            )
        }
    }


    fun serialize() = jsonSerializer.encodeToString(this)

    companion object {
        fun deserialize(it: String) = kotlin.runCatching {
            jsonSerializer.decodeFromString<AuthenticationResponseResultSerializable>(it)
        }.wrap()

        fun createInstance(result: AuthenticationResponseResult): AuthenticationResponseResultSerializable {
            return when(result) {
                is AuthenticationResponseResult.Post -> Post(
                    url = result.url,
                    params = result.params
                )
                is AuthenticationResponseResult.Redirect -> Redirect(
                    url = result.url,
                    params = result.params
                )
            }
        }
    }
}
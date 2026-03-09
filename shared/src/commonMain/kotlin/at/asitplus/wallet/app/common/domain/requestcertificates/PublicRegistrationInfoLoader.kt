package at.asitplus.wallet.app.common.domain.requestcertificates

import at.asitplus.signum.indispensable.josef.JwsCompactTyped
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.lib.jws.VerifyJwsSignature
import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

internal interface RegistrarBaseUrlResolver {
    fun resolveForRequestSource(requestUrl: String): String
}

internal class StaticRegistrarBaseUrlResolver(
    // default base url for local testing
    private val registrarBaseUrl: String = "http://10.0.2.2:5000"
) : RegistrarBaseUrlResolver {
    override fun resolveForRequestSource(requestUrl: String): String = registrarBaseUrl
}

internal class PublicRegistrationInfoLoader(
    private val httpService: HttpService,
    private val registrarBaseUrlResolver: RegistrarBaseUrlResolver = StaticRegistrarBaseUrlResolver(),
    private val chainValidator: WrpacCertificateChainValidator = WrpacCertificateChainValidator(),
) {
    private val tag = "[WRPRC] PublicRegistrationInfoLoader"

    suspend fun loadForRequestSource(requestUrl: String, wrpIdentifier: String? = null): List<JsonObject> {
        val registrarBaseUrl = registrarBaseUrlResolver.resolveForRequestSource(requestUrl)
        if (wrpIdentifier.isNullOrBlank()) {
            Napier.e("Public registration info lookup needs a WRP identifier once WRPRC is absent.", tag = tag)
            return emptyList()
        }

        Napier.i("WRPRC missing. Looking up public registration info via registrar $registrarBaseUrl for request source $requestUrl", tag = tag)
        val resolvedService = resolveServiceRegistration(requestUrl, registrarBaseUrl, wrpIdentifier)
        if (resolvedService == null) {
            Napier.e("No matching public WRP service registration found for request source $requestUrl and wrpIdentifier=$wrpIdentifier.", tag = tag)
            return emptyList()
        }

        Napier.i("Loading public registration info for wrp_id=${resolvedService.wrpIdentifier}, service_uri=${resolvedService.serviceUri}", tag = tag)
        val registrationInfoUrl = URLBuilder("$registrarBaseUrl/wrp/${resolvedService.wrpIdentifier}/registration-info")
            .apply { parameters.append("serviceUri", resolvedService.serviceUri) }
            .buildString()

        val registrationInfo = runCatching {
            loadSignedJsonObject(registrationInfoUrl)
        }.getOrElse {
            Napier.e("Could not load registration info from $registrationInfoUrl.", it, tag = tag)
            return emptyList()
        }

        val registrationInfoDto = runCatching {
            joseCompliantSerializer.decodeFromJsonElement<RegistrarRegistrationInfoDto>(registrationInfo)
        }.getOrElse {
            Napier.e("Could not decode registration info from $registrationInfoUrl.", it, tag = tag)
            return emptyList()
        }

        Napier.i(
            "Public registration info loaded for wrp_id=${registrationInfoDto.wrpIdentifier}, " +
                    "service_uri=${registrationInfoDto.serviceUri}, " +
                    "intendedUses=${registrationInfoDto.intendedUses.size}",
            tag = tag
        )
        Napier.d("Registration info payload: $registrationInfoDto", tag = tag)

        return listOf(
            buildJsonObject {
                put("iss", JsonPrimitive(registrationInfoDto.wrpIdentifier))
                put("sub", JsonPrimitive(registrationInfoDto.wrpIdentifier))
                registrationInfoDto.displayName?.takeIf { it.isNotBlank() }?.let { put("name", JsonPrimitive(it)) }
                put("registry_uri", JsonPrimitive(registrarBaseUrl.trimEnd('/')))
                put(
                    "srv_description",
                    buildJsonArray {
                        registrationInfoDto.displayName
                            ?.takeIf { it.isNotBlank() }
                            ?.let { displayName ->
                                add(
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("lang", JsonPrimitive("en"))
                                                put("value", JsonPrimitive(displayName))
                                            }
                                        )
                                    }
                                )
                            }
                    }
                )
                registrationInfoDto.intendedUses.firstNotNullOfOrNull { it.intendedUseIdentifier }
                    ?.let { put("intended_use_id", JsonPrimitive(it)) }
                registrationInfoDto.intendedUses.firstNotNullOfOrNull { it.privacyPolicy.singlePolicyUriOrNull() }
                    ?.let { put("privacy_policy", JsonPrimitive(it)) }
                put("credentials", registrationInfoDto.firstCredentialArrayOrEmpty())
            }
        )
    }

    private suspend fun resolveServiceRegistration(
        requestUrl: String,
        registrarBaseUrl: String,
        wrpIdentifier: String
    ): ResolvedServiceRegistration? {
        val requestOrigin = runCatching {
            val url = URLBuilder(requestUrl)
            val portSuffix = url.port.takeIf { it != url.protocol.defaultPort }?.let { ":$it" }.orEmpty()
            "${url.protocol.name}://${url.host}$portSuffix"
        }.getOrNull()

        val candidateUris = buildList {
            add(requestUrl)
            requestOrigin?.let { origin ->
                if (origin != requestUrl) {
                    add(origin)
                }
            }
        }

        candidateUris.forEach { serviceUri ->
            val serviceUrl = URLBuilder("$registrarBaseUrl/wrp/$wrpIdentifier/service")
                .apply { parameters.append("serviceUri", serviceUri) }
                .buildString()
            val exists = runCatching { loadSignedJsonObject(serviceUrl) }.getOrNull() != null
            if (exists) {
                return ResolvedServiceRegistration(wrpIdentifier, serviceUri)
            }
        }
        return null
    }

    private suspend fun loadSignedJsonObject(url: String): JsonObject {
        val body = httpService.buildHttpClient().use { httpClient ->
            httpClient.get(url).bodyAsText()
        }
        val jwsTyped = JwsCompactTyped<JsonElement>(body)

        val leafCertificate = chainValidator.validateMandatoryChain(
            jwsTyped.jws.jwsHeader.certificateChain,
            source = "registrar public API x5c"
        ) ?: error("Registrar public API JWS is missing a valid x5c chain.")
        VerifyJwsSignature().invoke(jwsTyped.jws, leafCertificate.decodedPublicKey.getOrThrow()).getOrThrow()
        return jwsTyped.payload as? JsonObject ?: error("Registrar public API JWS payload is not a JSON object.")
    }

    private fun RegistrarRegistrationInfoDto.firstCredentialArrayOrEmpty(): JsonArray =
        intendedUses.firstNotNullOfOrNull { intendedUse ->
            when (val credentials = intendedUse.credentials) {
                null -> null
                is JsonArray -> credentials.takeIf { it.isNotEmpty() }
                else -> buildJsonArray { add(credentials) }
            }
        } ?: buildJsonArray { }

    private fun JsonElement?.singlePolicyUriOrNull(): String? = when (this) {
        is JsonObject -> (this["policyURI"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        is JsonArray -> firstNotNullOfOrNull { element ->
            (element as? JsonObject)?.get("policyURI")?.let { policyUri ->
                (policyUri as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            }
        }
        else -> null
    }

    private data class ResolvedServiceRegistration(val wrpIdentifier: String, val serviceUri: String)
}

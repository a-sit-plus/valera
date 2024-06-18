package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.Holder
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.Verifier
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.iso.IssuerSigned
import at.asitplus.wallet.lib.oidc.AuthenticationResponseParameters
import at.asitplus.wallet.lib.oidc.OpenIdConstants
import at.asitplus.wallet.lib.oidc.OpenIdConstants.TOKEN_PREFIX_BEARER
import at.asitplus.wallet.lib.oidvci.CredentialFormatEnum
import at.asitplus.wallet.lib.oidvci.CredentialResponseParameters
import at.asitplus.wallet.lib.oidvci.IssuerMetadata
import at.asitplus.wallet.lib.oidvci.TokenResponseParameters
import at.asitplus.wallet.lib.oidvci.WalletService
import at.asitplus.wallet.lib.oidvci.decodeFromUrlQuery
import at.asitplus.wallet.lib.oidvci.encodeToParameters
import at.asitplus.wallet.lib.oidvci.formUrlEncode
import data.storage.DataStoreService
import data.storage.PersistentCookieStorage
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.util.flattenEntries
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okio.ByteString.Companion.decodeBase64

const val PATH_WELL_KNOWN_CREDENTIAL_ISSUER = "/.well-known/openid-credential-issuer"

val HttpHeaders.xAuthToken: String
    get() = "X-Auth-Token"

val OpenIdConstants.PARAMETER_STATE: String
    get() = "state"
val OpenIdConstants.PARAMETER_REDIRECT_URI: String
    get() = "redirect_uri"

class ProvisioningService(
    val platformAdapter: PlatformAdapter,
    private val dataStoreService: DataStoreService,
    private val cryptoService: CryptoService,
    private val holderAgent: HolderAgent,
    private val config: WalletConfig,
    errorService: ErrorService,
    httpService: HttpService,
) {
    var redirectUri: String? = null
    private val cookieStorage = PersistentCookieStorage(dataStoreService, errorService)
    private val client = httpService.buildHttpClient(cookieStorage = cookieStorage)

    @Throws(Throwable::class)
    suspend fun startProvisioning(
        host: String,
        credentialScheme: ConstantIndex.CredentialScheme,
        credentialRepresentation: ConstantIndex.CredentialRepresentation,
        requestedAttributes: Set<String>?,
    ) {
        config.set(
            host = host,
            credentialSchemeVcType = credentialScheme.vcType,
            credentialRepresentation = credentialRepresentation,
        )

        cookieStorage.reset()
        Napier.d("Start provisioning")

        runCatching {
            client.get("$host/oauth2/authorization/idaq")
        }.onSuccess { response ->
            val urlToOpen = response.headers[HttpHeaders.Location]

            val xAuthToken = response.headers[HttpHeaders.xAuthToken]
                ?: throw Exception("X-Auth-Token not received")

            if (urlToOpen != null) {
                val parameters = Url(urlToOpen).parameters
                val state = parameters[OpenIdConstants.PARAMETER_STATE]
                    ?: throw Exception("State not received")

                val redirectUri = parameters[OpenIdConstants.PARAMETER_REDIRECT_URI]
                this.redirectUri = redirectUri
                Napier.d("Set provisioningService.intentUrl to $redirectUri")
                if (redirectUri == null) {
                    throw Exception("Missing redirect uri")
                }

                val provisioningContext = ProvisioningContext(
                    redirectUri = redirectUri,
                    state = state,
                    xAuthToken = xAuthToken,
                    host = host,
                    credentialRepresentation = credentialRepresentation,
                    credentialSchemeVcType = credentialScheme.vcType,
                    requestedAttributes = requestedAttributes,
                )

                Napier.d("Store provisioning context: $provisioningContext")
                dataStoreService.setPreference(
                    key = Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT,
                    value = jsonSerializer.encodeToString(provisioningContext),
                )

                Napier.d("Open URL: $urlToOpen")
                platformAdapter.openUrl(urlToOpen)
            } else {
                throw Exception("Redirect not found in header")
            }
        }.onFailure {
            throw Exception(it)
        }
    }

    @Throws(Throwable::class)
    suspend fun handleResponse(link: String) {
        val fetchedProvisioningContext = dataStoreService.getPreference(
            key = Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT,
        ).firstOrNull() ?: throw Exception("Missing provisioning context")
        val provisioningContext =
            jsonSerializer.decodeFromString<ProvisioningContext>(fetchedProvisioningContext)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT)

        val credentialScheme = provisioningContext.credentialScheme
        val credentialRepresentation = provisioningContext.credentialRepresentation
        val host = provisioningContext.host
        val xAuthToken = provisioningContext.xAuthToken
        val requestedAttributes = provisioningContext.requestedAttributes

        val url = Url(link)
        url.parameters[OpenIdConstants.PARAMETER_STATE]?.let {
            if (it == provisioningContext.state) true else null
        } ?: throw Exception("Inconsistent provisioning state")

        Napier.d("Create request with x-auth: $xAuthToken")
        client.get(link) {
            headers[HttpHeaders.xAuthToken] = xAuthToken
        }

        Napier.d("Load X-Auth-Token: $xAuthToken")
        val metadata: IssuerMetadata = client.get("$host$PATH_WELL_KNOWN_CREDENTIAL_ISSUER") {
            headers[HttpHeaders.xAuthToken] = xAuthToken
        }.body()

        val oid4vciService = WalletService(
            clientId = host,
            cryptoService = cryptoService,
        )

        Napier.d("Oid4vciService.createAuthRequest")
        val requestOptions = WalletService.RequestOptions(
            credentialScheme = credentialScheme,
            representation = credentialRepresentation,
            requestedAttributes = requestedAttributes?.ifEmpty { null }
        )
        val authRequest = oid4vciService.createAuthRequest(
            requestOptions = requestOptions
        )

        val authorizationEndpointUrl = metadata.authorizationEndpointUrl
            ?: metadata.authorizationServers?.firstOrNull()
            ?: metadata.credentialIssuer
            ?: throw Exception("no authorizaitonEndpointUrl")
        Napier.d("HTTP.GET ($authorizationEndpointUrl)")
        val codeUrl = client.get(authorizationEndpointUrl) {
            authRequest.encodeToParameters().forEach {
                this.parameter(it.key, it.value)
            }
            headers[HttpHeaders.xAuthToken] = xAuthToken
        }.headers[HttpHeaders.Location]
            ?: throw Exception("codeUrl is null")

        val authnResponse = Url(codeUrl).parameters.flattenEntries().toMap()
            .decodeFromUrlQuery<AuthenticationResponseParameters>()
        val code = authnResponse.code
            ?: throw Exception("code is null")
        val tokenRequest =
            oid4vciService.createTokenRequestParameters(requestOptions, code, requestOptions.state)
        Napier.d("Created tokenRequest")
        val tokenResponse: TokenResponseParameters =
            client.submitForm(metadata.tokenEndpointUrl.toString()) {
                setBody(tokenRequest.encodeToParameters().formUrlEncode())
            }.body()

        Napier.d("Received tokenResponse")
        val credentialRequest =
            oid4vciService.createCredentialRequestJwt(
                requestOptions,
                tokenResponse.clientNonce,
                metadata.credentialIssuer
            ).getOrThrow()
        Napier.d("Created credentialRequest")
        val credentialResponse: CredentialResponseParameters =
            client.post(metadata.credentialEndpointUrl.toString()) {
                contentType(ContentType.Application.Json)
                setBody(credentialRequest)
                headers["Authorization"] = "$TOKEN_PREFIX_BEARER${tokenResponse.accessToken}"
            }.body()
        Napier.d("Received credentialResponse")

        credentialResponse.credential?.let {
            when (credentialResponse.format) {
                CredentialFormatEnum.NONE -> TODO("Function not implemented")
                CredentialFormatEnum.JWT_VC ->
                    holderAgent.storeCredentials(
                        listOf(
                            Holder.StoreCredentialInput.Vc(
                                vcJws = it,
                                scheme = credentialScheme,
                                attachments = null
                            )
                        )
                    )

                CredentialFormatEnum.JWT_VC_SD_UNOFFICIAL, CredentialFormatEnum.VC_SD_JWT ->
                    holderAgent.storeCredentials(
                        listOf(
                            Holder.StoreCredentialInput.SdJwt(
                                vcSdJwt = it,
                                scheme = credentialScheme
                            )
                        )
                    )

                CredentialFormatEnum.JWT_VC_JSON_LD -> TODO("Function not implemented")
                CredentialFormatEnum.JSON_LD -> TODO("Function not implemented")
                CredentialFormatEnum.MSO_MDOC -> {
                    it.decodeBase64()?.toByteArray()?.let {
                        IssuerSigned.deserialize(it)
                    }?.getOrNull()?.let { issuerSigned ->
                        holderAgent.storeCredentials(
                            listOf(
                                Holder.StoreCredentialInput.Iso(
                                    issuerSigned = issuerSigned,
                                    scheme = credentialScheme
                                )
                            )
                        )
                    } ?: throw Exception("Invalid credential format: $it")
                }
            }
        }?.let {
            if (it.rejected.isNotEmpty()) {
                throw Exception("Provisioning Failed for ${it.rejected.size}/${it.rejected.size + it.acceptedIso.size + it.acceptedSdJwt.size + it.acceptedVcJwt.size} credentials.")
            }
        }
    }
}

@Serializable
private data class ProvisioningContext(
    val state: String,
    val xAuthToken: String,
    val redirectUri: String,
    val host: String,
    val credentialRepresentation: ConstantIndex.CredentialRepresentation,
    private val credentialSchemeVcType: String,
    val requestedAttributes: Set<String>?,
) {
    val credentialScheme: ConstantIndex.CredentialScheme
        get() = AttributeIndex.resolveAttributeType(this.credentialSchemeVcType)
            ?: throw Exception("Unsupported credential scheme: ${this.credentialSchemeVcType}")
}
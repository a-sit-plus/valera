package at.asitplus.wallet.app.common

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.AuthenticationResponseParameters
import at.asitplus.openid.AuthorizationDetails
import at.asitplus.openid.CredentialFormatEnum
import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.CredentialResponseParameters
import at.asitplus.openid.IssuerMetadata
import at.asitplus.openid.OAuth2AuthorizationServerMetadata
import at.asitplus.openid.OpenIdConstants
import at.asitplus.openid.TokenRequestParameters
import at.asitplus.openid.TokenResponseParameters
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.Holder
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.iso.IssuerSigned
import at.asitplus.wallet.lib.jws.DefaultJwsService
import at.asitplus.wallet.lib.oauth2.OAuth2Client
import at.asitplus.wallet.lib.oidvci.WalletService
import at.asitplus.wallet.lib.oidvci.buildDPoPHeader
import at.asitplus.wallet.lib.oidvci.decodeFromUrlQuery
import at.asitplus.wallet.lib.oidvci.encodeToParameters
import com.benasher44.uuid.uuid4
import data.storage.DataStoreService
import data.storage.ExportableCredentialScheme.Companion.toExportableCredentialScheme
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.util.flattenEntries
import io.matthewnelson.encoding.base64.Base64
import io.matthewnelson.encoding.core.Decoder.Companion.decodeToByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Duration.Companion.minutes

class ProvisioningServiceVck(
    /**
     * Used to continue authentication in a web browser
     */
    private val openUrlExternally: suspend (String) -> Unit,
    private val client: HttpClient,
    private val dataStoreService: DataStoreService,
    private val cryptoService: CryptoService,
    private val holderAgent: HolderAgent,
    private val config: WalletConfig,
) {
    /** Checked by appLink handling whether to jump into [resumeWithAuthCode] */
    var redirectUri: String? = null

    private val redirectUrl = "asitplus-wallet://wallet.a-sit.at/app/callback"
    private val clientId = "https://wallet.a-sit.at/app"
    //private val redirectUrl = "eudi-openid4ci://authorize/"
    //private val clientId = "track2_full" // for authlete
    //private val clientId = "wallet-dev" // for EUDI

    private val oid4vciService =
        WalletService(clientId = clientId, cryptoService = cryptoService, redirectUrl = redirectUrl)
    private val jwsService = DefaultJwsService(cryptoService)

    /**
     * Loads credential metadata info from [host]
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialMetadata(
        host: String,
    ): Collection<CredentialIdentifierInfo> {
        Napier.d("Load credential metadata from $host")
        val credentialMetadata: IssuerMetadata = client.get("$host${OpenIdConstants.PATH_WELL_KNOWN_CREDENTIAL_ISSUER}").body()
        val supported = credentialMetadata.supportedCredentialConfigurations
            ?: throw Throwable("No supported credential configurations")
        return supported.mapNotNull {
            val identifier = it.key
            val scheme =
                it.value.credentialDefinition?.types?.firstNotNullOfOrNull { AttributeIndex.resolveAttributeType(it) }
                    ?: it.value.sdJwtVcType?.let { AttributeIndex.resolveSdJwtAttributeType(it) }
                    ?: it.value.docType?.let { AttributeIndex.resolveIsoDoctype(it) }
                    ?: return@mapNotNull null
            val attributes = it.value.credentialDefinition?.credentialSubject?.keys
                ?: it.value.sdJwtClaims?.keys
                ?: it.value.isoClaims?.flatMap { it.value.keys }
                ?: listOf()

            CredentialIdentifierInfo(
                credentialIdentifier = identifier,
                scheme = scheme.toExportableCredentialScheme(),
                attributes = attributes,
                supportedCredentialFormat = it.value
            )
        }
    }

    /**
     * Starts the issuing process at [credentialIssuer]
     */
    @Throws(Throwable::class)
    suspend fun startProvisioningWithAuthRequest(
        credentialIssuer: String,
        credentialIdentifierInfo: CredentialIdentifierInfo,
        requestedAttributes: Set<NormalizedJsonPath>?,
    ) {
        config.set(host = credentialIssuer)
        Napier.d("Start provisioning at $credentialIssuer with $credentialIdentifierInfo")
        // Load certificate, might trigger biometric prompt?
        CoroutineScope(Dispatchers.Unconfined).launch { cryptoService.keyMaterial.getCertificate() }

        val issuerMetadata: IssuerMetadata =
            client.get("$credentialIssuer${OpenIdConstants.PATH_WELL_KNOWN_CREDENTIAL_ISSUER}").body()
        val authorizationServer = issuerMetadata.authorizationServers?.firstOrNull() ?: credentialIssuer
        val oauthMetadataPath = "$authorizationServer${OpenIdConstants.PATH_WELL_KNOWN_OPENID_CONFIGURATION}"
        val oauthMetadata: OAuth2AuthorizationServerMetadata = client.get(oauthMetadataPath).body()

        val state = uuid4().toString()
        val requestedAttributeStrings = requestedAttributes?.map {
            // for now the attribute name is encoded at the first part
            (it.segments.first() as NormalizedJsonPathSegment.NameSegment).memberName
        }?.toSet()

        val authorizationEndpointUrl = oauthMetadata.authorizationEndpoint
            ?: throw Exception("no authorizationEndpoint in $oauthMetadata")
        val authorizationDetails = oid4vciService.buildAuthorizationDetails(
            credentialIdentifierInfo.credentialIdentifier,
            issuerMetadata.authorizationServers
        )
        storeProvisioningContext(
            state,
            credentialIssuer,
            credentialIdentifierInfo,
            requestedAttributeStrings,
            oauthMetadata,
            issuerMetadata
        )

        openAuthRequestInBrowser(
            state,
            authorizationDetails,
            authorizationEndpointUrl,
            oauthMetadata.pushedAuthorizationRequestEndpoint,
            credentialIssuer = credentialIssuer,
            push = oauthMetadata.requirePushedAuthorizationRequests ?: false,
        )
    }


    /**
     * Called after getting the redirect back from ID Austria to the Issuing Service
     */
    @Throws(Throwable::class)
    suspend fun resumeWithAuthCode(url: String) {
        Napier.d("resumeWithAuthCode with $url")
        this.redirectUri = null

        val provisioningContext = loadProvisioningContext()

        val state = provisioningContext.state
        val credentialIdentifierInfo = provisioningContext.credentialIdentifierInfo
        val credentialIdentifier = credentialIdentifierInfo.credentialIdentifier
        val issuerMetadata = provisioningContext.issuerMetadata
        val tokenEndpointUrl = provisioningContext.oauthMetadata.tokenEndpoint
            ?: throw Exception("no tokenEndpoint in ${provisioningContext.oauthMetadata}")
        val requestedAttributes = provisioningContext.requestedAttributes

        val authnResponse = Url(url).parameters.flattenEntries().toMap()
            .decodeFromUrlQuery<AuthenticationResponseParameters>()
        val code = authnResponse.code ?: throw Exception("code is null")

        val authorization = OAuth2Client.AuthorizationForToken.Code(code)
        val scope = credentialIdentifierInfo.supportedCredentialFormat.scope
        val tokenResponse: TokenResponseParameters =
            postAndLoadToken(state, issuerMetadata.credentialIssuer, authorization, scope, tokenEndpointUrl)

        Napier.d("Received tokenResponse")
        val authnDetails =
            tokenResponse.authorizationDetails?.filterIsInstance<AuthorizationDetails.OpenIdCredential>()?.firstOrNull()
        val input = if (authnDetails != null) {
            if (authnDetails.credentialConfigurationId != null)
            // TODO What about requested attributes?
                WalletService.CredentialRequestInput.CredentialIdentifier(credentialIdentifier)
            else
                WalletService.CredentialRequestInput.Format(
                    credentialIdentifierInfo.supportedCredentialFormat,
                    requestedAttributes
                )
        } else {
            WalletService.CredentialRequestInput.Format(
                credentialIdentifierInfo.supportedCredentialFormat,
                requestedAttributes
            )
        }

        val credentialScheme = credentialIdentifierInfo.scheme.toScheme()
        postCredentialRequestAndStore(input, tokenResponse, issuerMetadata, credentialScheme)
    }

    private suspend fun postAndLoadToken(
        state: String,
        credentialIssuer: String,
        authorization: OAuth2Client.AuthorizationForToken.Code,
        scope: String?,
        tokenEndpointUrl: String
    ): TokenResponseParameters = postToken(
        tokenEndpointUrl, credentialIssuer, oid4vciService.oauth2Client.createTokenRequestParameters(
            state = state,
            authorization = authorization,
            scope = scope,
        )
    )

    private suspend fun postAndLoadToken(
        state: String,
        credentialIssuer: String,
        authorization: OAuth2Client.AuthorizationForToken.PreAuthCode,
        authorizationDetails: Set<AuthorizationDetails.OpenIdCredential>,
        tokenEndpointUrl: String
    ): TokenResponseParameters = postToken(
        tokenEndpointUrl, credentialIssuer, oid4vciService.oauth2Client.createTokenRequestParameters(
            state = state,
            authorization = authorization,
            authorizationDetails = authorizationDetails
        )
    )

    private suspend fun postToken(
        tokenEndpointUrl: String,
        credentialIssuer: String,
        tokenRequest: TokenRequestParameters
    ): TokenResponseParameters {
        // TODO Decide when to set Attestation Header
        val clientAttestationJwt = jwsService.buildClientAttestationJwt(
            clientId = clientId,
            issuer = "https://example.com",
            lifetime = 60.minutes,
            clientKey = cryptoService.keyMaterial.jsonWebKey
        )
        val clientAttestationPoPJwt = jwsService.buildClientAttestationPoPJwt(
            clientId = clientId,
            audience = credentialIssuer,
            lifetime = 10.minutes,
        )
        // TODO Decide when to set DPoP header
        val dpopHeader = jwsService.buildDPoPHeader(
            url = tokenEndpointUrl,
        )
        return client.submitForm(
            url = tokenEndpointUrl,
            formParameters = parameters {
                tokenRequest.encodeToParameters<TokenRequestParameters>().forEach { append(it.key, it.value) }
            }
        ) {
            headers["OAuth-Client-Attestation"] = clientAttestationJwt.serialize()
            headers["OAuth-Client-Attestation-PoP"] = clientAttestationPoPJwt.serialize()
            headers["DPoP"] = dpopHeader
        }.body<TokenResponseParameters>()
    }

    private suspend fun postCredentialRequestAndStore(
        input: WalletService.CredentialRequestInput,
        tokenResponse: TokenResponseParameters,
        issuerMetadata: IssuerMetadata,
        credentialScheme: ConstantIndex.CredentialScheme
    ) {
        val credentialRequest = oid4vciService.createCredentialRequest(
            input, tokenResponse.clientNonce, issuerMetadata.credentialIssuer,
        ).getOrThrow()

        // TODO Decide when to set DPoP header
        val dpopHeader = jwsService.buildDPoPHeader(
            url = issuerMetadata.credentialEndpointUrl,
            accessToken = tokenResponse.accessToken
        )
        val credentialResponse: CredentialResponseParameters = client.post(issuerMetadata.credentialEndpointUrl) {
            contentType(ContentType.Application.Json)
            setBody(credentialRequest)
            headers["Authorization"] = "${tokenResponse.tokenType} ${tokenResponse.accessToken}"
            headers["DPoP"] = dpopHeader
        }.body()

        val storeCredentialInput = credentialResponse.credential
            ?.toStoreCredentialInput(credentialResponse.format, credentialScheme)
            ?: throw Exception("No credential was received")

        holderAgent.storeCredential(storeCredentialInput).getOrThrow()
    }

    /**
     * Decodes the content of a scanned QR code, expected to contain a [at.asitplus.openid.CredentialOffer].
     *
     * @param qrCodeContent as scanned
     */
    @Throws(Throwable::class)
    suspend fun decodeCredentialOffer(
        qrCodeContent: String
    ): CredentialOffer {
        val walletService = WalletService(
            cryptoService = cryptoService,
            remoteResourceRetriever = { url ->
                withContext(Dispatchers.IO) {
                    client.get(url).bodyAsText()
                }
            })
        return walletService.parseCredentialOffer(qrCodeContent).getOrThrow()
    }

    /**
     * Loads a user-selected credential with pre-authorized code from the OID4VCI credential issuer
     *
     * @param credentialOffer as loaded and decoded from the QR Code
     * @param credentialIdentifierInfo as selected by the user from the issuer's metadata
     * @param transactionCode if required from Issuing service, i.e. transmitted out-of-band to the user
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialWithOffer(
        credentialOffer: CredentialOffer,
        credentialIdentifierInfo: CredentialIdentifierInfo,
        transactionCode: String? = null,
        requestedAttributes: Set<NormalizedJsonPath>?
    ) {
        val credentialIssuer = credentialOffer.credentialIssuer
        val preAuthCode = credentialOffer.grants?.preAuthorizedCode?.preAuthorizedCode.toString()
        val issuerMetadata: IssuerMetadata = client.get("$credentialIssuer${OpenIdConstants.PATH_WELL_KNOWN_CREDENTIAL_ISSUER}").body()
        val authorizationServer = issuerMetadata.authorizationServers?.firstOrNull() ?: credentialIssuer
        val oauthMetadataPath = "$authorizationServer${OpenIdConstants.PATH_WELL_KNOWN_OPENID_CONFIGURATION}"
        val oauthMetadata: OAuth2AuthorizationServerMetadata = client.get(oauthMetadataPath).body()
        val tokenEndpointUrl = oauthMetadata.tokenEndpoint
            ?: throw Exception("no tokenEndpoint in $oauthMetadata")
        val state = uuid4().toString()
        val requestedAttributeStrings = requestedAttributes?.map {
            // for now the attribute name is encoded at the first part
            (it.segments.first() as NormalizedJsonPathSegment.NameSegment).memberName
        }?.toSet()

        credentialOffer.grants?.preAuthorizedCode?.let {
            val authorizationDetails = oid4vciService.buildAuthorizationDetails(
                credentialIdentifierInfo.credentialIdentifier,
                issuerMetadata.authorizationServers
            )

            val authorization = OAuth2Client.AuthorizationForToken.PreAuthCode(preAuthCode, transactionCode)
            val token: TokenResponseParameters = postAndLoadToken(
                state,
                issuerMetadata.credentialIssuer,
                authorization,
                authorizationDetails,
                tokenEndpointUrl
            )
            val input =
                WalletService.CredentialRequestInput.CredentialIdentifier(credentialIdentifierInfo.credentialIdentifier)

            postCredentialRequestAndStore(input, token, issuerMetadata, credentialIdentifierInfo.scheme.toScheme())
        } ?: credentialOffer.grants?.authorizationCode?.let {
            storeProvisioningContext(
                state,
                credentialIssuer,
                credentialIdentifierInfo,
                requestedAttributeStrings,
                oauthMetadata,
                issuerMetadata
            )

            val authorizationEndpointUrl = oauthMetadata.authorizationEndpoint
                ?: throw Exception("no authorizationEndpoint in $oauthMetadata")
            val authorizationDetails = oid4vciService.buildAuthorizationDetails(
                credentialIdentifierInfo.credentialIdentifier,
                issuerMetadata.authorizationServers
            )
            openAuthRequestInBrowser(
                state,
                authorizationDetails,
                authorizationEndpointUrl,
                oauthMetadata.pushedAuthorizationRequestEndpoint,
                credentialIssuer = credentialIssuer,
                issuerState = it.issuerState,
                push = oauthMetadata.requirePushedAuthorizationRequests ?: false
            )
        } ?: {
            throw Exception("No offer grants received in ${credentialOffer.grants}")
        }
    }

    private fun String.toStoreCredentialInput(
        format: CredentialFormatEnum?,
        credentialScheme: ConstantIndex.CredentialScheme,
    ) = when (format) {
        CredentialFormatEnum.JWT_VC -> Holder.StoreCredentialInput.Vc(this, credentialScheme)

        CredentialFormatEnum.VC_SD_JWT -> Holder.StoreCredentialInput.SdJwt(this, credentialScheme)

        CredentialFormatEnum.MSO_MDOC -> kotlin.runCatching { decodeToByteArray(Base64()) }.getOrNull()
            ?.let { IssuerSigned.deserialize(it) }?.getOrNull()
            ?.let { Holder.StoreCredentialInput.Iso(it, credentialScheme) }
            ?: throw Exception("Invalid credential format: $this")

        else -> {
            if (contains("~")) {
                Holder.StoreCredentialInput.SdJwt(this, credentialScheme)
            } else runCatching { decodeToByteArray(Base64()) }.getOrNull()
                ?.let { IssuerSigned.deserialize(it) }?.getOrNull()
                ?.let { Holder.StoreCredentialInput.Iso(it, credentialScheme) }
                ?: Holder.StoreCredentialInput.Vc(this, credentialScheme)
        }
    }

    private suspend fun storeProvisioningContext(
        state: String,
        credentialIssuer: String,
        credentialIdentifierInfo: CredentialIdentifierInfo,
        requestedAttributeStrings: Set<String>?,
        oauthMetadata: OAuth2AuthorizationServerMetadata,
        issuerMetadata: IssuerMetadata,
    ) {
        val provisioningContext = ProvisioningContext(
            state,
            credentialIssuer,
            credentialIdentifierInfo,
            requestedAttributeStrings,
            oauthMetadata,
            issuerMetadata
        )
        Napier.d("Store provisioning context: $provisioningContext")
        dataStoreService.setPreference(
            key = Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT,
            value = vckJsonSerializer.encodeToString(provisioningContext),
        )
    }

    private suspend fun openAuthRequestInBrowser(
        state: String,
        authorizationDetails: Set<AuthorizationDetails.OpenIdCredential>,
        authorizationEndpointUrl: String,
        pushedAuthorizationRequestEndpoint: String?,
        credentialIssuer: String,
        issuerState: String? = null,
        push: Boolean = false
    ) {
        val authRequest =
            oid4vciService.oauth2Client.createAuthRequest(state, authorizationDetails, issuerState = issuerState)
        val authorizationUrl = if (pushedAuthorizationRequestEndpoint != null && push) {
            // TODO Decide when to set Attestation Header
            val clientAttestationJwt = jwsService.buildClientAttestationJwt(
                clientId = clientId,
                issuer = "https://example.com",
                lifetime = 60.minutes,
                clientKey = cryptoService.keyMaterial.jsonWebKey
            )
            val clientAttestationPoPJwt = jwsService.buildClientAttestationPoPJwt(
                clientId = clientId,
                audience = credentialIssuer,
                lifetime = 10.minutes,
            )
            val response = client.submitForm(
                url = pushedAuthorizationRequestEndpoint,
                formParameters = parameters {
                    authRequest.encodeToParameters().forEach { append(it.key, it.value) }
                    append("prompt", "login")
                }
            ) {
                headers["OAuth-Client-Attestation"] = clientAttestationJwt.serialize()
                headers["OAuth-Client-Attestation-PoP"] = clientAttestationPoPJwt.serialize()
            }.body<JsonObject>()

            // format is {"expires_in":3600,"request_uri":"urn:uuid:c330d8b1-6ecb-4437-8818-cbca64d2e710"}
            (response["error_description"] as? JsonPrimitive?)?.contentOrNull
                ?.let { throw Exception(it) }
            (response["error"] as? JsonPrimitive?)?.contentOrNull
                ?.let { throw Exception(it) }
            val requestUri = (response["request_uri"] as? JsonPrimitive?)?.contentOrNull
                ?: throw Exception("No request_uri from PAR response")
            URLBuilder(authorizationEndpointUrl).also { builder ->
                builder.parameters.append("client_id", clientId)
                builder.parameters.append("request_uri", requestUri)
                builder.parameters.append("state", state)
            }.build().toString()
        } else {
            URLBuilder(authorizationEndpointUrl).also { builder ->
                authRequest.encodeToParameters<AuthenticationRequestParameters>().forEach {
                    builder.parameters.append(it.key, it.value)
                }
                builder.parameters.append("prompt", "login")
            }.build().toString()
        }
        Napier.d("Provisioning starts by opening URL $authorizationUrl")
        this.redirectUri = redirectUrl
        openUrlExternally.invoke(authorizationUrl)
    }

    private suspend fun loadProvisioningContext(): ProvisioningContext =
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT).firstOrNull()
            ?.let {
                vckJsonSerializer.decodeFromString<ProvisioningContext>(it)
                    .also { dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT) }
            }
            ?: throw Exception("Missing provisioning context")

}
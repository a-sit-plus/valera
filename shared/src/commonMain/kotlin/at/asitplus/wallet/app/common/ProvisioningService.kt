package at.asitplus.wallet.app.common

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.AuthenticationResponseParameters
import at.asitplus.openid.AuthorizationDetails
import at.asitplus.openid.CredentialFormatEnum
import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.CredentialOfferGrants
import at.asitplus.openid.CredentialOfferGrantsPreAuthCodeTransactionCode
import at.asitplus.openid.CredentialResponseParameters
import at.asitplus.openid.IssuerMetadata
import at.asitplus.openid.OAuth2AuthorizationServerMetadata
import at.asitplus.openid.OpenIdConstants.PATH_WELL_KNOWN_CREDENTIAL_ISSUER
import at.asitplus.openid.OpenIdConstants.PATH_WELL_KNOWN_OPENID_CONFIGURATION
import at.asitplus.openid.TokenRequestParameters
import at.asitplus.openid.TokenResponseParameters
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.identifier
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.Holder
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.iso.IssuerSigned
import at.asitplus.wallet.lib.oauth2.OAuth2Client
import at.asitplus.wallet.lib.oidvci.WalletService
import at.asitplus.wallet.lib.oidvci.decodeFromCredentialIdentifier
import at.asitplus.wallet.lib.oidvci.decodeFromUrlQuery
import at.asitplus.wallet.lib.oidvci.encodeToParameters
import at.asitplus.wallet.lib.oidvci.toRepresentation
import com.benasher44.uuid.uuid4
import data.storage.DataStoreService
import data.storage.ExportableCredentialScheme
import data.storage.ExportableCredentialScheme.Companion.toExportableCredentialScheme
import data.storage.PersistentCookieStorage
import io.github.aakira.napier.Napier
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive


class ProvisioningService(
    val platformAdapter: PlatformAdapter,
    private val dataStoreService: DataStoreService,
    private val cryptoService: CryptoService,
    private val holderAgent: HolderAgent,
    private val config: WalletConfig,
    errorService: ErrorService,
    httpService: HttpService,
) {
    /** Checked by appLink handling whether to jump into [resumeProvisioningWithAuthCode] */
    var redirectUri: String? = null
    private val cookieStorage = PersistentCookieStorage(dataStoreService, errorService)
    private val client = httpService.buildHttpClient(cookieStorage = cookieStorage)
    private val redirectUrl = "asitplus-wallet://wallet.a-sit.at/app/callback"
    private val clientId = "wallet-dev"
    private val oid4vciService =
        WalletService(clientId = clientId, cryptoService = cryptoService, redirectUrl = redirectUrl)

    /**
     * Starts the issuing process at [credentialIssuer]
     */
    @Throws(Throwable::class)
    suspend fun startProvisioning(
        credentialIssuer: String,
        credentialScheme: ConstantIndex.CredentialScheme,
        credentialRepresentation: ConstantIndex.CredentialRepresentation,
        requestedAttributes: Set<NormalizedJsonPath>?,
    ) {
        config.set(credentialIssuer, credentialRepresentation, credentialScheme.identifier)
        cookieStorage.reset()
        Napier.d("Start provisioning at $credentialIssuer")
        // Load certificate, might trigger biometric prompt?
        CoroutineScope(Dispatchers.Unconfined).launch { cryptoService.keyMaterial.getCertificate() }

        val credentialMetadata: IssuerMetadata =
            client.get("$credentialIssuer$PATH_WELL_KNOWN_CREDENTIAL_ISSUER").body()
        val authorizationServer = credentialMetadata.authorizationServers?.firstOrNull() ?: credentialIssuer
        val oauthMetadataPath = "$authorizationServer$PATH_WELL_KNOWN_OPENID_CONFIGURATION"
        val oauthMetadata: OAuth2AuthorizationServerMetadata = client.get(oauthMetadataPath).body()

        val state = uuid4().toString()
        val requestedAttributeStrings = requestedAttributes?.map {
            // for now the attribute name is encoded at the first part
            (it.segments.first() as NormalizedJsonPathSegment.NameSegment).memberName
        }?.toSet()

        storeProvisioningContext(
            state,
            credentialRepresentation,
            credentialScheme,
            requestedAttributeStrings,
            credentialIssuer,
            oauthMetadata.tokenEndpoint
                ?: throw Exception("no tokenEndpoint in $oauthMetadata"),
            credentialMetadata.serialize()
        )
        val requestOptions = WalletService.RequestOptions(credentialScheme, credentialRepresentation)
        val scope = oid4vciService.buildScope(requestOptions, credentialMetadata)
        val authorizationEndpointUrl = oauthMetadata.authorizationEndpoint
            ?: throw Exception("no authorizationEndpoint in $oauthMetadata")
        openAuthRequestInBrowser(
            state,
            authorizationEndpointUrl,
            null,
            scope,
            oauthMetadata.pushedAuthorizationRequestEndpoint,
        )
    }

    /**
     * Called after getting the redirect back from ID Austria to the Issuing Service
     */
    @Throws(Throwable::class)
    suspend fun resumeProvisioningWithAuthCode(redirectedUrl: String) {
        Napier.d("handleResponse with $redirectedUrl")
        // should start with "https://wallet.a-sit.at/mobile/callback"
        val provisioningContext = loadAndRemoveProvisioningContext()
        this.redirectUri = null

        val requestOptions = with(provisioningContext) {
            WalletService.RequestOptions(credentialScheme, credentialRepresentation)
        }
        val scope = with(provisioningContext) {
            oid4vciService.buildScope(requestOptions, issuerMetadata)
                ?: throw Exception("Can't build scope for $credentialScheme, $credentialRepresentation and $issuerMetadata")
        }

        val authnResponse = Url(redirectedUrl).parameters.flattenEntries().toMap()
            .decodeFromUrlQuery<AuthenticationResponseParameters>()
        val code = authnResponse.code ?: throw Exception("code is null")
        val authorization = OAuth2Client.AuthorizationForToken.Code(code)

        val tokenResponse = with(provisioningContext) {
            postAndLoadToken(state, authorization, null, scope, tokenEndpointUrl)
        }

        val input = WalletService.CredentialRequestInput.RequestOptions(requestOptions)
        with(provisioningContext) {
            postCredentialAndStore(
                input,
                tokenResponse,
                credentialIssuer,
                issuerMetadata.credentialEndpointUrl,
                credentialScheme,
                credentialRepresentation
            )
        }
    }

    /**
     * Decodes the content of a scanned QR code, expected to contain a [at.asitplus.openid.CredentialOffer].
     *
     * @param qrCodeContent as scanned
     */
    @Throws(Throwable::class)
    suspend fun decodeCredentialOffer(
        qrCodeContent: String
    ): CredentialOfferInfo {
        val walletService = WalletService(
            cryptoService = cryptoService,
            remoteResourceRetriever = { url ->
                withContext(Dispatchers.IO) {
                    client.get(url).bodyAsText()
                }
            })
        val credentialOffer = walletService.parseCredentialOffer(qrCodeContent).getOrThrow()
        val mappedCredentials = credentialOffer.configurationIds
            .mapNotNull { ma ->
                decodeFromCredentialIdentifier(ma)?.let {
                    ma to Pair(it.first.toExportableCredentialScheme(), it.second)
                } ?: decodeFromEudiCredentialIdentifier(ma)?.let {
                    ma to Pair(it.first.toExportableCredentialScheme(), it.second)
                }
            }
            .toMap()
        return CredentialOfferInfo(
            credentialOffer,
            mappedCredentials,
            credentialOffer.grants?.preAuthorizedCode?.transactionCode
        )
    }

    /**
     * Decodes Identifiers used in the EUDI Wallet Reference Backend
     * in the form of `eu.europa.ec.eudi.pid_vc_sd_jwt` into a scheme known by our implementation
     */
    // TODO Rework
    private fun decodeFromEudiCredentialIdentifier(
        input: String
    ): Pair<ConstantIndex.CredentialScheme, CredentialFormatEnum>? {
        if (input.contains("_")) {
            val vcTypeOrSdJwtType = input.substringBefore("_")
            val formatString = input.substringAfter("_")
                .replace("vc_sd_jwt", CredentialFormatEnum.VC_SD_JWT.text)
                .replace("jwt_vc_json", CredentialFormatEnum.VC_SD_JWT.text)
                .replace("^mdoc$".toRegex(), CredentialFormatEnum.MSO_MDOC.text)
            val credentialScheme = AttributeIndex.resolveSdJwtAttributeType(vcTypeOrSdJwtType)
                ?: AttributeIndex.resolveAttributeType(vcTypeOrSdJwtType)
                ?: AttributeIndex.resolveIsoNamespace(vcTypeOrSdJwtType)
                ?: return null
            val format = CredentialFormatEnum.parse(formatString)
                ?: return null
            return Pair(credentialScheme, format)
        } else {
            return AttributeIndex.resolveIsoNamespace(input)
                ?.let { Pair(it, CredentialFormatEnum.MSO_MDOC) }
        }
    }

    /**
     * Loads a user-selected credential with pre-authorized code from the OID4VCI credential issuer
     *
     * @param credentialIssuer from [at.asitplus.openid.CredentialOffer.credentialIssuer]
     * @param offerGrants from [at.asitplus.openid.CredentialOffer.grants], extracted from the received offer
     * @param credentialIdToRequest one from [at.asitplus.openid.CredentialOffer.configurationIds]
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialWithOffer(
        credentialIssuer: String,
        offerGrants: CredentialOfferGrants?,
        credentialIdToRequest: String,
        credentialRepresentation: ConstantIndex.CredentialRepresentation,
        transactionCode: String?,
    ) {
        val credentialScheme = decodeFromCredentialIdentifier(credentialIdToRequest)?.first
            ?: decodeFromEudiCredentialIdentifier(credentialIdToRequest)?.first
            ?: throw Exception("can't resolve credential scheme")

        offerGrants?.preAuthorizedCode?.let {
            val issuerMetadata: IssuerMetadata =
                client.get("$credentialIssuer$PATH_WELL_KNOWN_CREDENTIAL_ISSUER").body()
            val authorizationServer = issuerMetadata.authorizationServers?.firstOrNull() ?: credentialIssuer
            val oauthMetadataPath = "$authorizationServer$PATH_WELL_KNOWN_OPENID_CONFIGURATION"
            val oauthMetadata: OAuth2AuthorizationServerMetadata = client.get(oauthMetadataPath).body()
            val tokenEndpointUrl = oauthMetadata.tokenEndpoint
                ?: throw Exception("no tokenEndpoint in $oauthMetadata")

            val state = uuid4().toString()
            val authorizationDetails =
                oid4vciService.buildAuthorizationDetails(credentialIdToRequest, issuerMetadata.authorizationServers)
            val authorization = OAuth2Client.AuthorizationForToken.PreAuthCode(it.preAuthorizedCode, transactionCode)
            val tokenResponse = postAndLoadToken(state, authorization, authorizationDetails, null, tokenEndpointUrl)
            val input = WalletService.CredentialRequestInput.CredentialIdentifier(credentialIdToRequest)

            with(issuerMetadata) {
                postCredentialAndStore(
                    input,
                    tokenResponse,
                    credentialIssuer,
                    credentialEndpointUrl,
                    credentialScheme,
                    credentialRepresentation
                )
            }
        } ?: offerGrants?.authorizationCode?.let {
            val issuerMetadata: IssuerMetadata =
                client.get("$credentialIssuer$PATH_WELL_KNOWN_CREDENTIAL_ISSUER").body()
            val authorizationServer = it.authorizationServer
                ?: issuerMetadata.authorizationServers?.first()
                ?: credentialIssuer
            val oauthMetadataPath = "$authorizationServer$PATH_WELL_KNOWN_OPENID_CONFIGURATION"
            val oauthMetadata: OAuth2AuthorizationServerMetadata = client.get(oauthMetadataPath).body()
            val state = uuid4().toString()

            storeProvisioningContext(
                state,
                credentialRepresentation,
                credentialScheme,
                null,
                credentialIssuer,
                oauthMetadata.tokenEndpoint
                    ?: throw Exception("no tokenEndpoint in $oauthMetadata"),
                issuerMetadata.serialize()
            )

            val authorizationEndpointUrl = oauthMetadata.authorizationEndpoint
                ?: throw Exception("no authorizationEndpoint in $oauthMetadata")

            openAuthRequestInBrowser(
                state,
                authorizationEndpointUrl,
                null,
                credentialIdToRequest,
                oauthMetadata.pushedAuthorizationRequestEndpoint,
            )
        } ?: {
            throw Exception("No offer grants received in $offerGrants")
        }
    }

    private suspend fun storeProvisioningContext(
        state: String,
        credentialRepresentation: ConstantIndex.CredentialRepresentation,
        credentialScheme: ConstantIndex.CredentialScheme,
        requestedAttributeStrings: Set<String>?,
        credentialIssuer: String,
        tokenEndpointUrl: String,
        issuerMetadataString: String,
    ) {
        ProvisioningContext(
            state = state,
            credentialRepresentation = credentialRepresentation,
            credentialSchemeIdentifier = credentialScheme.identifier,
            requestedAttributes = requestedAttributeStrings,
            credentialIssuer = credentialIssuer,
            tokenEndpointUrl = tokenEndpointUrl,
            issuerMetadataString = issuerMetadataString,
        ).also {
            Napier.d("Store provisioning context: $it")
            dataStoreService.setPreference(
                key = Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT,
                value = vckJsonSerializer.encodeToString(it),
            )
        }

    }

    private suspend fun openAuthRequestInBrowser(
        state: String,
        authorizationEndpointUrl: String,
        authorizationDetails: Set<AuthorizationDetails.OpenIdCredential>? = null,
        scope: String? = null,
        pushedAuthorizationRequestEndpoint: String?,
    ) {
        val authRequest = oid4vciService.oauth2Client.createAuthRequest(state, authorizationDetails, scope)
        val authorizationUrl = if (pushedAuthorizationRequestEndpoint != null) {
            val response = client.submitForm(
                url = pushedAuthorizationRequestEndpoint,
                formParameters = parameters {
                    authRequest.encodeToParameters().forEach { append(it.key, it.value) }
                    append("prompt", "login")
                }
            ).body<JsonObject>()
            // format is {"expires_in":3600,"request_uri":"urn:uuid:c330d8b1-6ecb-4437-8818-cbca64d2e710"}
            URLBuilder(authorizationEndpointUrl).also { builder ->
                builder.parameters.append("client_id", clientId)
                builder.parameters.append("request_uri", (response.getValue("request_uri") as JsonPrimitive).content)
                builder.parameters.append("state", state)
            }.build().toString()
        } else {
            URLBuilder(authorizationEndpointUrl).also { builder ->
                authRequest.encodeToParameters<AuthenticationRequestParameters>().forEach {
                    builder.parameters.append(it.key, it.value)
                }
            }.build().toString()
        }
        Napier.d("Provisioning starts by opening URL $authorizationUrl")
        this.redirectUri = redirectUrl
        platformAdapter.openUrl(authorizationUrl)
    }

    private suspend fun loadAndRemoveProvisioningContext(): ProvisioningContext {
        return dataStoreService.getPreference(
            Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT,
        ).firstOrNull()
            ?.let { vckJsonSerializer.decodeFromString<ProvisioningContext>(it) }
            ?.also { dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT) }
            ?: throw Exception("Missing provisioning context")
    }

    private suspend fun postCredentialAndStore(
        input: WalletService.CredentialRequestInput,
        tokenResponse: TokenResponseParameters,
        credentialIssuer: String,
        credentialEndpointUrl: String,
        credentialScheme: ConstantIndex.CredentialScheme,
        credentialRepresentation: ConstantIndex.CredentialRepresentation
    ): Holder.StoredCredential {
        val credentialRequest = oid4vciService.createCredentialRequest(
            input = input,
            clientNonce = tokenResponse.clientNonce,
            credentialIssuer = credentialIssuer
        ).getOrThrow()

        val credentialResponse: CredentialResponseParameters = client.post(credentialEndpointUrl) {
            contentType(ContentType.Application.Json)
            setBody(credentialRequest)
            headers["Authorization"] = "${tokenResponse.tokenType} ${tokenResponse.accessToken}"
        }.body()

        val storeCredentialInput = credentialResponse.credential
            ?.toStoreCredentialInput(
                credentialResponse.format?.toRepresentation() ?: credentialRepresentation,
                credentialScheme
            )
            ?: throw Exception("No credential was received")

        return holderAgent.storeCredential(storeCredentialInput).getOrThrow()
    }

    private suspend fun postAndLoadToken(
        state: String,
        authorization: OAuth2Client.AuthorizationForToken,
        authorizationDetails: Set<AuthorizationDetails>? = null,
        scope: String? = null,
        tokenEndpointUrl: String
    ): TokenResponseParameters {
        val tokenRequest = oid4vciService.oauth2Client.createTokenRequestParameters(
            state = state,
            authorization = authorization,
            authorizationDetails = authorizationDetails,
            scope = scope,
        )
        return client.submitForm(
            url = tokenEndpointUrl,
            formParameters = parameters {
                tokenRequest.encodeToParameters<TokenRequestParameters>().forEach { append(it.key, it.value) }
            }
        ).body<TokenResponseParameters>()
    }

    private fun String.toStoreCredentialInput(
        representation: ConstantIndex.CredentialRepresentation,
        credentialScheme: ConstantIndex.CredentialScheme,
    ) = when (representation) {
        ConstantIndex.CredentialRepresentation.PLAIN_JWT -> Holder.StoreCredentialInput.Vc(this, credentialScheme)
        ConstantIndex.CredentialRepresentation.SD_JWT -> Holder.StoreCredentialInput.SdJwt(this, credentialScheme)
        ConstantIndex.CredentialRepresentation.ISO_MDOC -> {
            runCatching { decodeToByteArray(Base64()) }.getOrNull()
                ?.let { IssuerSigned.deserialize(it) }
                ?.getOrElse { throw it }
                ?.let { Holder.StoreCredentialInput.Iso(it, credentialScheme) }
                ?: throw Exception("Invalid credential format for ISO_MDOC: $this")
        }
    }

}

@Serializable
private data class ProvisioningContext(
    val state: String,
    val credentialRepresentation: ConstantIndex.CredentialRepresentation,
    private val credentialSchemeIdentifier: String,
    val requestedAttributes: Set<String>?,
    val credentialIssuer: String,
    val tokenEndpointUrl: String,
    val issuerMetadataString: String,
) {
    val credentialScheme: ConstantIndex.CredentialScheme
        get() = AttributeIndex.resolveCredential(this.credentialSchemeIdentifier)?.first
            ?: throw Exception("Unsupported credential scheme: ${this.credentialSchemeIdentifier}")
    val issuerMetadata: IssuerMetadata
        get() = IssuerMetadata.deserialize(issuerMetadataString).getOrThrow()
}

@Serializable
data class CredentialOfferInfo(
    /**
     * The credential offer as parsed
     */
    val credentialOffer: CredentialOffer,
    /**
     * Maps entries from [at.asitplus.openid.CredentialOffer.configurationIds] to resolved credential scheme
     */
    val credentials: Map<String, Pair<ExportableCredentialScheme, CredentialFormatEnum>>,
    /**
     * Maybe the user needs to enter a transaction code
     */
    val transactionCode: CredentialOfferGrantsPreAuthCodeTransactionCode? = null,
)

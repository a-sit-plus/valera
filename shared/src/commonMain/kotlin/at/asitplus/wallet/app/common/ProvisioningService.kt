package at.asitplus.wallet.app.common

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.openid.AuthenticationResponseParameters
import at.asitplus.openid.AuthorizationDetails
import at.asitplus.openid.CredentialFormatEnum
import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.CredentialOfferGrantsPreAuthCode
import at.asitplus.openid.CredentialResponseParameters
import at.asitplus.openid.IssuerMetadata
import at.asitplus.openid.OAuth2AuthorizationServerMetadata
import at.asitplus.openid.OpenIdConstants.PATH_WELL_KNOWN_CREDENTIAL_ISSUER
import at.asitplus.openid.OpenIdConstants.PATH_WELL_KNOWN_OPENID_CONFIGURATION
import at.asitplus.openid.TokenRequestParameters
import at.asitplus.openid.TokenResponseParameters
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.Holder
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.iso.IssuerSigned
import at.asitplus.wallet.lib.oauth2.OAuth2Client
import at.asitplus.wallet.lib.oidvci.WalletService
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okio.ByteString.Companion.decodeBase64

class ProvisioningService(
    val platformAdapter: PlatformAdapter,
    private val dataStoreService: DataStoreService,
    private val cryptoService: CryptoService,
    private val holderAgent: HolderAgent,
    private val config: WalletConfig,
    errorService: ErrorService,
    httpService: HttpService,
) {
    /** Checked by appLink handling whether to jump into [resumeWithAuthCode] */
    var redirectUri: String? = null
    private val cookieStorage = PersistentCookieStorage(dataStoreService, errorService)
    private val client = httpService.buildHttpClient(cookieStorage = cookieStorage)
    private val redirectUrl = "asitplus-wallet://wallet.a-sit.at/app/callback"
    private val clientId = "https://wallet.a-sit.at/app"
    private val oid4vciService =
        WalletService(clientId = clientId, cryptoService = cryptoService, redirectUrl = redirectUrl)

    @Serializable
    data class CredentialIdentifierInfo(
        val credentialIdentifier: String,
        val scope: String?,
        val scheme: ExportableCredentialScheme,
        val representation: ConstantIndex.CredentialRepresentation,
        val attributes: Collection<String>,
    )

    /**
     * Loads credential metadata info from [host]
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialMetadata(
        host: String,
    ): Collection<CredentialIdentifierInfo> {
        Napier.d("Load credential metadata from $host")
        val credentialMetadata: IssuerMetadata = client.get("$host$PATH_WELL_KNOWN_CREDENTIAL_ISSUER").body()
        val supported = credentialMetadata.supportedCredentialConfigurations
            ?: throw Throwable("No supported credential configurations")
        return supported.mapNotNull {
            val identifier = it.key
            val representation = it.value.format.toRepresentation()
            val scope = it.value.scope
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
                identifier,
                scope,
                scheme.toExportableCredentialScheme(),
                representation,
                attributes
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
        cookieStorage.reset()
        Napier.d("Start provisioning at $credentialIssuer with $credentialIdentifierInfo")
        // Load certificate, might trigger biometric prompt?
        CoroutineScope(Dispatchers.Unconfined).launch { cryptoService.keyMaterial.getCertificate() }

        val issuerMetadata: IssuerMetadata =
            client.get("$credentialIssuer$PATH_WELL_KNOWN_CREDENTIAL_ISSUER").body()
        val authorizationServer = issuerMetadata.authorizationServers?.firstOrNull() ?: credentialIssuer
        val oauthMetadataPath = "$authorizationServer$PATH_WELL_KNOWN_OPENID_CONFIGURATION"
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

        openAuthRequestInBrowser(state, authorizationDetails, authorizationEndpointUrl)
    }


    /**
     * Called after getting the redirect back from ID Austria to the Issuing Service
     */
    @Throws(Throwable::class)
    suspend fun resumeWithAuthCode(redirectedUrl: String) {
        Napier.d("handleResponse with $redirectedUrl")
        this.redirectUri = null

        val provisioningContext = loadProvisioningContext()

        val state = provisioningContext.state
        val credentialIdentifierInfo = provisioningContext.credentialIdentifierInfo
        val credentialIdentifier = credentialIdentifierInfo.credentialIdentifier
        val issuerMetadata = provisioningContext.issuerMetadata
        val tokenEndpointUrl = provisioningContext.oauthMetadata.tokenEndpoint
            ?: throw Exception("no tokenEndpoint in ${provisioningContext.oauthMetadata}")
        val requestedAttributes = provisioningContext.requestedAttributes // TODO use them

        val authnResponse = Url(redirectedUrl).parameters.flattenEntries().toMap()
            .decodeFromUrlQuery<AuthenticationResponseParameters>()
        val code = authnResponse.code ?: throw Exception("code is null")

        val authorization = OAuth2Client.AuthorizationForToken.Code(code)
        val scope = credentialIdentifierInfo.scope
        val tokenResponse: TokenResponseParameters =
            postAndLoadToken(state, authorization, scope, tokenEndpointUrl)

        Napier.d("Received tokenResponse")
        val input = WalletService.CredentialRequestInput.CredentialIdentifier(credentialIdentifier) // TODO use format?

        val credentialScheme = credentialIdentifierInfo.scheme.toScheme()
        postCredentialRequestAndStore(input, tokenResponse, issuerMetadata, credentialScheme)
    }

    private suspend fun postAndLoadToken(
        state: String,
        authorization: OAuth2Client.AuthorizationForToken.Code,
        scope: String?,
        tokenEndpointUrl: String
    ): TokenResponseParameters = postToken(
        tokenEndpointUrl, oid4vciService.oauth2Client.createTokenRequestParameters(
            state = state,
            authorization = authorization,
            scope = scope,
        )
    )

    private suspend fun postAndLoadToken(
        state: String,
        authorization: OAuth2Client.AuthorizationForToken.PreAuthCode,
        authorizationDetails: Set<AuthorizationDetails.OpenIdCredential>,
        tokenEndpointUrl: String
    ): TokenResponseParameters = postToken(
        tokenEndpointUrl, oid4vciService.oauth2Client.createTokenRequestParameters(
            state = state,
            authorization = authorization,
            authorizationDetails = authorizationDetails
        )
    )

    private suspend fun postToken(
        tokenEndpointUrl: String,
        tokenRequest: TokenRequestParameters
    ): TokenResponseParameters = client.submitForm(
        url = tokenEndpointUrl,
        formParameters = parameters {
            tokenRequest.encodeToParameters<TokenRequestParameters>().forEach { append(it.key, it.value) }
        }
    ).body<TokenResponseParameters>()

    private suspend fun postCredentialRequestAndStore(
        input: WalletService.CredentialRequestInput.CredentialIdentifier,
        tokenResponse: TokenResponseParameters,
        issuerMetadata: IssuerMetadata,
        credentialScheme: ConstantIndex.CredentialScheme
    ) {
        val credentialRequest = oid4vciService.createCredentialRequest(
            input, tokenResponse.clientNonce, issuerMetadata.credentialIssuer,
        ).getOrThrow()

        val credentialResponse: CredentialResponseParameters = client.post(issuerMetadata.credentialEndpointUrl) {
            contentType(ContentType.Application.Json)
            setBody(credentialRequest)
            headers["Authorization"] = "${tokenResponse.tokenType} ${tokenResponse.accessToken}"
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
     * @param credentialIssuer from [at.asitplus.openid.CredentialOffer.credentialIssuer]
     * @param preAuthorizedCode from [at.asitplus.openid.CredentialOffer.grants], more specifically [CredentialOfferGrantsPreAuthCode.preAuthorizedCode]
     * @param credentialIdentifier one from [at.asitplus.openid.CredentialOffer.configurationIds]
     * @param transactionCode if required from Issuing service, i.e. transmitted out-of-band to the user
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialWithPreAuthn(
        credentialIssuer: String,
        preAuthorizedCode: String,
        credentialIdentifier: String,
        transactionCode: String? = null,
        requestedAttributes: Set<NormalizedJsonPath>?,
        credentialScheme: ExportableCredentialScheme
    ) {
        val issuerMetadata: IssuerMetadata = client.get("$credentialIssuer$PATH_WELL_KNOWN_CREDENTIAL_ISSUER").body()
        val authorizationServer = issuerMetadata.authorizationServers?.firstOrNull() ?: credentialIssuer
        val oauthMetadataPath = "$authorizationServer$PATH_WELL_KNOWN_OPENID_CONFIGURATION"
        val oauthMetadata: OAuth2AuthorizationServerMetadata = client.get(oauthMetadataPath).body()
        val tokenEndpointUrl = oauthMetadata.tokenEndpoint
            ?: throw Exception("no tokenEndpoint in $oauthMetadata")
        val state = uuid4().toString()
        val authorizationDetails =
            oid4vciService.buildAuthorizationDetails(credentialIdentifier, issuerMetadata.authorizationServers)

        val authorization = OAuth2Client.AuthorizationForToken.PreAuthCode(preAuthorizedCode, transactionCode)
        val token: TokenResponseParameters =
            postAndLoadToken(state, authorization, authorizationDetails, tokenEndpointUrl)

        val requestedAttributeStrings = requestedAttributes?.map {
            // for now the attribute name is encoded at the first part
            (it.segments.first() as NormalizedJsonPathSegment.NameSegment).memberName
        }?.toSet() // TODO use requested attributes

        val input = WalletService.CredentialRequestInput.CredentialIdentifier(credentialIdentifier)

        postCredentialRequestAndStore(input, token, issuerMetadata, credentialScheme.toScheme())
    }

    private fun String.toStoreCredentialInput(
        format: CredentialFormatEnum?,
        credentialScheme: ConstantIndex.CredentialScheme,
    ) = when (format) {
        CredentialFormatEnum.JWT_VC -> Holder.StoreCredentialInput.Vc(this, credentialScheme)

        CredentialFormatEnum.VC_SD_JWT -> Holder.StoreCredentialInput.SdJwt(this, credentialScheme)

        CredentialFormatEnum.MSO_MDOC -> {
            decodeBase64()?.toByteArray()?.let {
                IssuerSigned.deserialize(it)
            }?.getOrNull()?.let { issuerSigned ->
                Holder.StoreCredentialInput.Iso(issuerSigned, credentialScheme)
            } ?: throw Exception("Invalid credential format: $this")
        }

        else -> throw Exception("Invalid credential format: $this")
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
        authorizationEndpointUrl: String
    ) {
        val authRequest = oid4vciService.oauth2Client.createAuthRequest(state, authorizationDetails)
        val authorizationUrl = URLBuilder(authorizationEndpointUrl).also { builder ->
            authRequest.encodeToParameters().forEach {
                builder.parameters.append(it.key, it.value)
            }
        }.build().toString()
        Napier.d("Provisioning starts by opening URL $authorizationUrl")
        this.redirectUri = redirectUrl
        platformAdapter.openUrl(authorizationUrl)
    }

    private suspend fun loadProvisioningContext(): ProvisioningContext =
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT).firstOrNull()
            ?.let {
                vckJsonSerializer.decodeFromString<ProvisioningContext>(it)
                    .also { dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT) }
            }
            ?: throw Exception("Missing provisioning context")

}

@Serializable
private data class ProvisioningContext(
    val state: String,
    val host: String,
    val credentialIdentifierInfo: ProvisioningService.CredentialIdentifierInfo,
    val requestedAttributes: Set<String>?,
    val oauthMetadata: OAuth2AuthorizationServerMetadata,
    val issuerMetadata: IssuerMetadata,
)


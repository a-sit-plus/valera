package at.asitplus.wallet.app.common

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.openid.AuthenticationResponseParameters
import at.asitplus.openid.CredentialFormatEnum
import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.CredentialOfferGrants
import at.asitplus.openid.CredentialOfferGrantsPreAuthCodeTransactionCode
import at.asitplus.openid.CredentialResponseParameters
import at.asitplus.openid.IssuerMetadata
import at.asitplus.openid.OAuth2AuthorizationServerMetadata
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

const val PATH_WELL_KNOWN_CREDENTIAL_ISSUER = "/.well-known/openid-credential-issuer"
const val PATH_WELL_KNOWN_AUTH_SERVER = "/.well-known/openid-configuration"

class ProvisioningService(
    val platformAdapter: PlatformAdapter,
    private val dataStoreService: DataStoreService,
    private val cryptoService: CryptoService,
    private val holderAgent: HolderAgent,
    private val config: WalletConfig,
    errorService: ErrorService,
    httpService: HttpService,
) {
    /** Checked by appLink handling whether to jump into [handleResponse] */
    var redirectUri: String? = null
    private val cookieStorage = PersistentCookieStorage(dataStoreService, errorService)
    private val client = httpService.buildHttpClient(cookieStorage = cookieStorage)
    private val redirectUrl = "asitplus-wallet://wallet.a-sit.at/app/callback"
    private val oid4vciService = WalletService(cryptoService = cryptoService, redirectUrl = redirectUrl)

    /**
     * Starts the issuing process at [host]
     */
    @Throws(Throwable::class)
    suspend fun startProvisioning(
        host: String,
        credentialScheme: ConstantIndex.CredentialScheme,
        credentialRepresentation: ConstantIndex.CredentialRepresentation,
        requestedAttributes: Set<NormalizedJsonPath>?,
    ) {
        config.set(
            host = host,
            credentialSchemeIdentifier = credentialScheme.identifier,
            credentialRepresentation = credentialRepresentation,
        )
        cookieStorage.reset()
        Napier.d("Start provisioning at $host")
        // Load certificate, might trigger biometric prompt?
        CoroutineScope(Dispatchers.Unconfined).launch { cryptoService.keyMaterial.getCertificate() }

        val credentialMetadata: IssuerMetadata = client.get("$host$PATH_WELL_KNOWN_CREDENTIAL_ISSUER").body()
        // TODO common code
        val authorizationServer = credentialMetadata.authorizationServers?.firstOrNull() ?: host
        val oauthMetadataPath = "$authorizationServer$PATH_WELL_KNOWN_AUTH_SERVER"
        val oauthMetadata: OAuth2AuthorizationServerMetadata = client.get(oauthMetadataPath).body()
        val tokenEndpointUrl = oauthMetadata.tokenEndpoint
            ?: throw Exception("no tokenEndpoint in $oauthMetadata")

        val state = uuid4().toString()
        val requestedAttributeStrings = requestedAttributes?.map {
            // for now the attribute name is encoded at the first part
            (it.segments.first() as NormalizedJsonPathSegment.NameSegment).memberName
        }?.toSet()


        val requestOptions = WalletService.RequestOptions(
            credentialScheme = credentialScheme,
            representation = credentialRepresentation,
            requestedAttributes = requestedAttributeStrings?.ifEmpty { null },
        )
        val authorizationDetails = oid4vciService.buildAuthorizationDetails(requestOptions)
        val provisioningContext = ProvisioningContext(
            state = state,
            credentialRepresentation = credentialRepresentation,
            credentialSchemeIdentifier = credentialScheme.identifier,
            requestedAttributes = requestedAttributeStrings,
            credentialIssuer = host,
            tokenEndpointUrl = tokenEndpointUrl,
            credentialEndpointUrl = credentialMetadata.credentialEndpointUrl,
        )

        val authorizationEndpointUrl = oauthMetadata.authorizationEndpoint
            ?: throw Exception("no authorizationEndpoint in $oauthMetadata")
        Napier.d("Store provisioning context: $provisioningContext")
        dataStoreService.setPreference(
            key = Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT,
            value = vckJsonSerializer.encodeToString(provisioningContext),
        )

        val authRequest = oid4vciService.oauth2Client.createAuthRequest(state, authorizationDetails)
        val authorizationUrl = URLBuilder(authorizationEndpointUrl).also { builder ->
            authRequest.encodeToParameters().forEach {
                builder.parameters.append(it.key, it.value)
            }
        }.build().toString()
        Napier.d("Provisioning starts by opening URL $authorizationUrl")
        this.redirectUri = redirectUrl
        platformAdapter.openUrl(authorizationUrl)
        return
    }

    /**
     * Called after getting the redirect back from ID Austria to the Issuing Service
     */
    @Throws(Throwable::class)
    suspend fun handleResponse(redirectedUrl: String) {
        Napier.d("handleResponse with $redirectedUrl")
        // should start with "https://wallet.a-sit.at/mobile/callback"
        val provisioningContext = dataStoreService.getPreference(
            Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT,
        ).firstOrNull()
            ?.let { vckJsonSerializer.decodeFromString<ProvisioningContext>(it) }
            ?: throw Exception("Missing provisioning context")
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT)
        this.redirectUri = null

        val requestOptions = WalletService.RequestOptions(
            credentialScheme = provisioningContext.credentialScheme,
            representation = provisioningContext.credentialRepresentation,
            requestedAttributes = provisioningContext.requestedAttributes,
        )
        val authorizationDetails = oid4vciService.buildAuthorizationDetails(requestOptions)

        val authnResponse = Url(redirectedUrl).parameters.flattenEntries().toMap()
            .decodeFromUrlQuery<AuthenticationResponseParameters>()
        val code = authnResponse.code ?: throw Exception("code is null")
        val tokenRequest = oid4vciService.oauth2Client.createTokenRequestParameters(
            state = provisioningContext.state,
            authorization = OAuth2Client.AuthorizationForToken.Code(code),
            authorizationDetails = authorizationDetails,
        )

        Napier.d("Created tokenRequest")
        val tokenResponse: TokenResponseParameters = client.submitForm(
            url = provisioningContext.tokenEndpointUrl,
            formParameters = parameters {
                tokenRequest.encodeToParameters().forEach { append(it.key, it.value) }
            }
        ).body()

        Napier.d("Received tokenResponse")
        val credentialRequest = oid4vciService.createCredentialRequest(
            input = WalletService.CredentialRequestInput.RequestOptions(requestOptions),
            clientNonce = tokenResponse.clientNonce,
            credentialIssuer = provisioningContext.credentialIssuer,
        ).getOrThrow()
        Napier.d("Created credentialRequest")
        val credentialResponse: CredentialResponseParameters = client.post(provisioningContext.credentialEndpointUrl) {
            contentType(ContentType.Application.Json)
            setBody(credentialRequest)
            headers["Authorization"] = "${tokenResponse.tokenType} ${tokenResponse.accessToken}"
        }.body()
        Napier.d("Received credentialResponse")

        val storeCredentialInput = credentialResponse.credential
            ?.toStoreCredentialInput(credentialResponse.format, provisioningContext.credentialScheme)
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
        return CredentialOfferInfo(credentialOffer, mappedCredentials, credentialOffer.grants?.preAuthorizedCode?.transactionCode)
    }

    /**
     * Decodes Identifiers used in the EUDI Wallet Reference Backend
     * in the form of `eu.europa.ec.eudi.pid_vc_sd_jwt` into a scheme known by our implementation
     */
    private fun decodeFromEudiCredentialIdentifier(
        input: String
    ): Pair<ConstantIndex.CredentialScheme, CredentialFormatEnum>? {
        if (input.contains("_")) {
            val vcTypeOrSdJwtType = input.substringBefore("_")
            val formatString = input.substringAfter("_")
                .replace("vc_sd_jwt", "vc+sd-jwt")
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
    ) {
        val credentialScheme = decodeFromCredentialIdentifier(credentialIdToRequest)?.first
            ?: decodeFromEudiCredentialIdentifier(credentialIdToRequest)?.first
            ?: throw Exception("can't resolve credential scheme")
        val walletService = WalletService(cryptoService = cryptoService)

        offerGrants?.preAuthorizedCode?.let {
            val issuerMetadata: IssuerMetadata = client.get("$credentialIssuer$PATH_WELL_KNOWN_CREDENTIAL_ISSUER").body()
            val authorizationServer = issuerMetadata.authorizationServers?.firstOrNull() ?: credentialIssuer
            val oauthMetadataPath = "$authorizationServer$PATH_WELL_KNOWN_AUTH_SERVER"
            val oauthMetadata: OAuth2AuthorizationServerMetadata = client.get(oauthMetadataPath).body()
            val tokenEndpointUrl = oauthMetadata.tokenEndpoint
                ?: throw Exception("no tokenEndpoint in $oauthMetadata")

            val state = uuid4().toString()
            val tokenRequest = walletService.oauth2Client.createTokenRequestParameters(
                state = state,
                authorization = OAuth2Client.AuthorizationForToken.PreAuthCode(it.preAuthorizedCode),
                authorizationDetails = walletService.buildAuthorizationDetails(
                    credentialIdToRequest,
                    issuerMetadata.authorizationServers
                )
            )
            val token: TokenResponseParameters = client.submitForm(
                url = tokenEndpointUrl,
                formParameters = parameters {
                    tokenRequest.encodeToParameters().forEach { append(it.key, it.value) }
                }
            ).body()
            val credentialRequest = walletService.createCredentialRequest(
                input = WalletService.CredentialRequestInput.CredentialIdentifier(credentialIdToRequest),
                clientNonce = token.clientNonce,
                credentialIssuer = issuerMetadata.credentialIssuer
            ).getOrThrow()

            val credentialResponse: CredentialResponseParameters = client.post(issuerMetadata.credentialEndpointUrl) {
                contentType(ContentType.Application.Json)
                setBody(credentialRequest)
                headers["Authorization"] = "${token.tokenType} ${token.accessToken}"
            }.body()

            val storeCredentialInput = credentialResponse.credential
                ?.toStoreCredentialInput(credentialResponse.format, credentialScheme)
                ?: throw Exception("No credential was received")

            holderAgent.storeCredential(storeCredentialInput).getOrThrow()
        } ?: offerGrants?.authorizationCode?.let {
            val issuerMetadata: IssuerMetadata = client.get("$credentialIssuer$PATH_WELL_KNOWN_CREDENTIAL_ISSUER").body()
            val authorizationServer = it.authorizationServer
                ?: issuerMetadata.authorizationServers?.first()
                ?: credentialIssuer
            val oauthMetadataPath = "$authorizationServer$PATH_WELL_KNOWN_AUTH_SERVER"
            val oauthMetadata: OAuth2AuthorizationServerMetadata = client.get(oauthMetadataPath).body()
            val tokenEndpointUrl = oauthMetadata.tokenEndpoint
                ?: throw Exception("no tokenEndpoint in $oauthMetadata")
            val state = uuid4().toString()

            val authorizationDetails = oid4vciService.buildAuthorizationDetails(credentialIdToRequest, setOf(authorizationServer))
            val provisioningContext = ProvisioningContext(
                state = state,
                credentialRepresentation = credentialRepresentation,
                credentialSchemeIdentifier = credentialScheme.identifier,
                requestedAttributes = null,
                credentialIssuer = credentialIssuer,
                tokenEndpointUrl = tokenEndpointUrl,
                credentialEndpointUrl = issuerMetadata.credentialEndpointUrl,
            )

            val authorizationEndpointUrl = oauthMetadata.authorizationEndpoint
                ?: throw Exception("no authorizationEndpoint in $oauthMetadata")
            Napier.d("Store provisioning context: $provisioningContext")
            dataStoreService.setPreference(
                key = Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT,
                value = vckJsonSerializer.encodeToString(provisioningContext),
            )

            val authRequest = oid4vciService.oauth2Client.createAuthRequest(state, authorizationDetails)
            val authorizationUrl = URLBuilder(authorizationEndpointUrl).also { builder ->
                authRequest.encodeToParameters().forEach {
                    builder.parameters.append(it.key, it.value)
                }
            }.build().toString()
            Napier.d("Provisioning starts by opening URL $authorizationUrl")
            this.redirectUri = redirectUrl
            platformAdapter.openUrl(authorizationUrl)
            return
        } ?: {
            throw Exception("No offer grants received in $offerGrants")
        }
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

}

@Serializable
private data class ProvisioningContext(
    val state: String,
    val credentialRepresentation: ConstantIndex.CredentialRepresentation,
    private val credentialSchemeIdentifier: String,
    val requestedAttributes: Set<String>?,
    val credentialIssuer: String,
    val tokenEndpointUrl: String,
    val credentialEndpointUrl: String,
) {
    val credentialScheme: ConstantIndex.CredentialScheme
        get() = AttributeIndex.resolveCredential(this.credentialSchemeIdentifier)?.first
            ?: throw Exception("Unsupported credential scheme: ${this.credentialSchemeIdentifier}")
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

package at.asitplus.wallet.app.common

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.IssuerMetadata
import at.asitplus.openid.OAuth2AuthorizationServerMetadata
import at.asitplus.openid.SupportedCredentialFormat
import at.asitplus.signum.indispensable.io.Base64UrlStrict
import at.asitplus.signum.indispensable.josef.ConfirmationClaim
import at.asitplus.signum.indispensable.josef.JsonWebKey
import at.asitplus.signum.indispensable.josef.JsonWebToken
import at.asitplus.signum.indispensable.josef.JwsHeader
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.jws.JwsService
import at.asitplus.wallet.lib.oidvci.WalletService
import data.storage.DataStoreService
import data.storage.ExportableCredentialScheme
import data.storage.ExportableCredentialScheme.Companion.toExportableCredentialScheme
import data.storage.PersistentCookieStorage
import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class ProvisioningService(
    val platformAdapter: PlatformAdapter,
    private val dataStoreService: DataStoreService,
    private val cryptoService: CryptoService,
    holderAgent: HolderAgent,
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
    //private val redirectUrl = "eudi-openid4ci://authorize/"
    //private val clientId = "track2_full" // for authlete
    //private val clientId = "wallet-dev" // for EUDI

    private val provisioningService = ProvisioningServiceVck(
        openUrlExternally = {
            this.redirectUri = redirectUrl
            platformAdapter.openUrl(it)
        },
        client = client,
        storeProvisioningContext = {
            dataStoreService.setPreference(
                key = Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT,
                value = vckJsonSerializer.encodeToString(it),
            )
        },
        loadProvisioningContext = {
            dataStoreService.getPreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT).firstOrNull()
                ?.let {
                    vckJsonSerializer.decodeFromString<ProvisioningContext>(it)
                        .also { dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT) }
                }
        },
        cryptoService = cryptoService,
        holderAgent = holderAgent,
        redirectUrl = redirectUrl,
        clientId = clientId,
    )

    /**
     * Loads credential metadata info from [host]
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialMetadata(
        host: String,
    ): Collection<CredentialIdentifierInfo> {
        return provisioningService.loadCredentialMetadata(host)
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
        provisioningService.startProvisioningWithAuthRequest(
            credentialIssuer,
            credentialIdentifierInfo,
            requestedAttributes
        )
    }


    /**
     * Called after getting the redirect back from ID Austria to the Issuing Service
     */
    @Throws(Throwable::class)
    suspend fun resumeWithAuthCode(redirectedUrl: String) {
        Napier.d("handleResponse with $redirectedUrl")
        this.redirectUri = null
        provisioningService.resumeWithAuthCode(redirectedUrl)
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
        provisioningService.loadCredentialWithOffer(
            credentialOffer,
            credentialIdentifierInfo,
            transactionCode,
            requestedAttributes
        )
    }

}

@Serializable
data class ProvisioningContext(
    val state: String,
    val host: String,
    val credentialIdentifierInfo: CredentialIdentifierInfo,
    val requestedAttributes: Set<String>?,
    val oauthMetadata: OAuth2AuthorizationServerMetadata,
    val issuerMetadata: IssuerMetadata,
)


@Serializable
data class CredentialIdentifierInfo(
    val credentialIdentifier: String,
    val scheme: ExportableCredentialScheme,
    val attributes: Collection<String>,
    val supportedCredentialFormat: SupportedCredentialFormat,
) {
    constructor(
        credentialIdentifier: String,
        scheme: ConstantIndex.CredentialScheme,
        attributes: Collection<String>,
        supportedCredentialFormat: SupportedCredentialFormat,
    ) : this(
        credentialIdentifier,
        scheme.toExportableCredentialScheme(),
        attributes,
        supportedCredentialFormat
    )
}

/**
 * Client attestation JWT, issued by the backend service to a client, which can be sent to an OAuth2 Authorization
 * Server if needed, e.g. as HTTP header `OAuth-Client-Attestation`, see
 * [OAuth 2.0 Attestation-Based Client Authentication](https://www.ietf.org/archive/id/draft-ietf-oauth-attestation-based-client-auth-04.html)
 *
 * @param issuer a unique identifier for the entity that issued the JWT
 */
// TODO Will be included in vck 5.2.0
suspend fun JwsService.buildClientAttestationJwt(
    clientId: String,
    issuer: String,
    lifetime: Duration,
    clientKey: JsonWebKey,
) = createSignedJwsAddingParams(
    header = JwsHeader(
        algorithm = algorithm,
        type = "oauth-client-attestation+jwt"
    ),
    payload = JsonWebToken(
        issuer = issuer,
        subject = clientId,
        issuedAt = Clock.System.now() - 5.minutes,
        expiration = Clock.System.now() + lifetime,
        confirmationClaim = ConfirmationClaim(
            jsonWebKey = clientKey,
        )
    ),
    serializer = JsonWebToken.serializer(),
    addKeyId = false,
    addJsonWebKey = false,
    addX5c = false,
).getOrThrow()

/**
 * Client attestation PoP JWT, issued by the client, which can be sent to an OAuth2 Authorization Server if needed,
 * e.g. as HTTP header `OAuth-Client-Attestation-PoP`, see
 * [OAuth 2.0 Attestation-Based Client Authentication](https://www.ietf.org/archive/id/draft-ietf-oauth-attestation-based-client-auth-04.html)
 *
 * @param audience The RFC8414 issuer identifier URL of the authorization server MUST be used
 */
// TODO Will be included in vck 5.2.0
suspend fun JwsService.buildClientAttestationPoPJwt(
    clientId: String,
    audience: String,
    lifetime: Duration,
    nonce: String? = null,
) = createSignedJwsAddingParams(
    header = JwsHeader(
        algorithm = algorithm,
        type = "oauth-client-attestation-pop+jwt"
    ),
    payload = JsonWebToken(
        issuer = clientId,
        audience = audience,
        jwtId = Random.nextBytes(12).encodeToString(Base64UrlStrict),
        nonce = nonce,
        issuedAt = Clock.System.now() - 5.minutes,
        expiration = Clock.System.now() + lifetime,
    ),
    serializer = JsonWebToken.serializer(),
    addKeyId = false,
    addJsonWebKey = false,
    addX5c = false,
).getOrThrow()

package at.asitplus.wallet.app.common

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.josef.JsonWebKey
import at.asitplus.signum.indispensable.josef.JsonWebKeySet
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_update_action
import at.asitplus.valera.resources.snackbar_update_hint
import at.asitplus.wallet.app.common.dcapi.CredentialsContainer
import at.asitplus.wallet.app.common.dcapi.DCAPIRequest
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.cbor.DefaultCoseService
import at.asitplus.wallet.lib.jws.DefaultJwsService
import at.asitplus.wallet.lib.jws.DefaultVerifierJwsService
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import at.asitplus.wallet.lib.rqes.Initializer.initRqesModule
import data.storage.AntilogAdapter
import data.storage.DataStoreService
import data.storage.PersistentSubjectCredentialStore
import getImageDecoder
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.swiftzer.semver.SemVer
import org.jetbrains.compose.resources.getString

/**
 * Main class to hold all services needed in the Compose App.
 */
class WalletMain(
    val cryptoService: WalletCryptoService,
    private val dataStoreService: DataStoreService,
    val platformAdapter: PlatformAdapter,
    var subjectCredentialStore: PersistentSubjectCredentialStore = PersistentSubjectCredentialStore(
        dataStoreService
    ),
    val buildContext: BuildContext,
    val scope: CoroutineScope
) {
    lateinit var walletConfig: WalletConfig
    lateinit var holderAgent: HolderAgent
    lateinit var provisioningService: ProvisioningService
    lateinit var httpService: HttpService
    lateinit var presentationService: PresentationService
    lateinit var snackbarService: SnackbarService
    lateinit var signingService: SigningService
    lateinit var errorService: ErrorService
    lateinit var dcApiService: DCAPIService
    private val regex = Regex("^(?=\\[[0-9]{2})", option = RegexOption.MULTILINE)

    val readyForIntents = MutableStateFlow<Boolean?>(null)

    init {
        at.asitplus.wallet.mdl.Initializer.initWithVCK()
        at.asitplus.wallet.idaustria.Initializer.initWithVCK()
        at.asitplus.wallet.eupid.Initializer.initWithVCK()
        at.asitplus.wallet.cor.Initializer.initWithVCK()
        at.asitplus.wallet.por.Initializer.initWithVCK()
        at.asitplus.wallet.companyregistration.Initializer.initWithVCK()
        at.asitplus.wallet.healthid.Initializer.initWithVCK()
        at.asitplus.wallet.taxid.Initializer.initWithVCK()
        initRqesModule()
        Napier.takeLogarithm()
        Napier.base(AntilogAdapter(platformAdapter, "", buildContext.buildType))
    }

    @Throws(Throwable::class)
    fun initialize(snackbarService: SnackbarService) {
        val coseService = DefaultCoseService(cryptoService)
        walletConfig =
            WalletConfig(dataStoreService = this.dataStoreService, errorService = errorService)
        subjectCredentialStore = PersistentSubjectCredentialStore(dataStoreService)
        holderAgent = HolderAgent(
            validator = Validator(
                verifierJwsService = DefaultVerifierJwsService(
                    publicKeyLookup = issuerKeyLookup()
                )
            ),
            subjectCredentialStore = subjectCredentialStore,
            jwsService = DefaultJwsService(cryptoService),
            coseService = coseService,
            keyPair = cryptoService.keyMaterial,
        )

        httpService = HttpService(buildContext)
        provisioningService = ProvisioningService(
            platformAdapter,
            dataStoreService,
            cryptoService,
            holderAgent,
            walletConfig,
            errorService,
            httpService
        )
        presentationService = PresentationService(
            platformAdapter,
            cryptoService,
            holderAgent,
            httpService,
            coseService
        )
        signingService = SigningService(platformAdapter, dataStoreService, errorService, snackbarService, httpService)
        this.snackbarService = snackbarService
        this.dcApiService = DCAPIService(platformAdapter)
    }

    private fun issuerKeyLookup(): (JwsSigned<*>) -> Set<JsonWebKey>? = { jws ->
        if (jws.payloadIssuer() == "https://dss.aegean.gr/rfc-issuer" && jws.header.keyId != null) {
            loadJsonWebKeySet("https://dss.aegean.gr/.well-known/")
                ?.keys?.firstOrNull { it.keyId == jws.header.keyId }?.let { setOf(it) }
        } else setOf()
    }

    private fun loadJsonWebKeySet(url: String) = runBlocking {
        CoroutineScope(Dispatchers.IO).async {
            catchingUnwrapped {
                httpService.buildHttpClient().get(url).body<JsonWebKeySet>()
            }.getOrNull()
        }.await()
    }

    private fun JwsSigned<*>.payloadIssuer() =
        ((payload as? JsonObject?)?.get("iss") as? JsonPrimitive?)?.content

    suspend fun resetApp() {
        dataStoreService.clearLog()

        subjectCredentialStore.reset()

        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_VCS)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_COOKIES)
        walletConfig.reset()
    }

    fun getLog(): List<String> {
        val rawLog = platformAdapter.readFromFile("log.txt", "logs")
        return rawLog?.split(regex = regex)?.filter { it.isNotEmpty() } ?: listOf("")
    }

    fun clearLog() {
        dataStoreService.clearLog()
    }

    fun startProvisioning(
        host: String,
        credentialIdentifierInfo: CredentialIdentifierInfo,
        onSuccess: () -> Unit,
    ) {
        scope.launch {
            try {
                provisioningService.startProvisioningWithAuthRequest(
                    credentialIssuer = host,
                    credentialIdentifierInfo = credentialIdentifierInfo,
                )
                onSuccess()
            } catch (e: Throwable) {
                errorService.emit(e)
            }
        }
    }

    fun updateDigitalCredentialsAPIIntegration() {
        scope.launch {
            try {
                Napier.d("Updating digital credentials integration")
                subjectCredentialStore.observeStoreContainer().collect { container ->
                    dcApiService.registerCredentialWithSystem(container)
                }
            } catch (e: Throwable) {
                Napier.w("Could not update credentials with system", e)
            }
        }
    }

    fun updateCheck() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val httpClient = httpService.buildHttpClient()
                val host = "https://wallet.a-sit.at/"
                val url = "${host}check.json"
                Napier.d("Getting check.json from $url")
                val json = httpClient.get(url).body<JsonObject>()
                json["apps"]?.jsonObject?.get(buildContext.packageName)?.let {
                    (it as? JsonObject)?.get("latestVersion")?.jsonPrimitive?.content?.let {
                        val latestVersion = SemVer.parse(it)
                        val currentVersion = SemVer.parse(buildContext.versionName)
                        Napier.d("Version is $currentVersion, latest is $latestVersion")
                        if (latestVersion > currentVersion) {
                            snackbarService.showSnackbar(
                                getString(
                                    Res.string.snackbar_update_hint,
                                    host,
                                    latestVersion
                                ), getString(Res.string.snackbar_update_action)
                            ) {
                                platformAdapter.openUrl(host)
                            }
                        }
                    }
                }
            }.onFailure {
                Napier.w("Update check failed", it)
            }
        }
    }
}

fun PlatformAdapter.decodeImage(image: ByteArray): ImageBitmap {
    return getImageDecoder((image))
}

/**
 * Adapter to call back to native code without the need for service objects
 */
interface PlatformAdapter {
    /**
     * Opens a specified resource (Intent, Associated Domain)
     */
    fun openUrl(url: String)

    /**
     * Converts an image from ByteArray to ImageBitmap
     * @param image the image as ByteArray
     * @return returns the image as an ImageBitmap
     */

    /**
     * Writes an user defined string to a file in a specific folder
     * @param text is the content of the new file or the content which gets append to an existing file
     * @param fileName the name of the file
     * @param folderName the name of the folder in which the file resides
     */
    fun writeToFile(text: String, fileName: String, folderName: String)

    /**
     * Reads the content from a file in a specific folder
     * @param fileName the name of the file
     * @param folderName the name of the folder in which the file resides
     * @return returns the content of the file
     */
    fun readFromFile(fileName: String, folderName: String): String?

    /**
     * Clears the content of a file
     * @param fileName the name of the file
     * @param folderName the name of the folder in which the file resides
     */
    fun clearFile(fileName: String, folderName: String)

    /**
     * Opens the platform specific share dialog
     */
    fun shareLog()

    /**
     * Registers credentials with the digital credentials browser API
     * @param entries credentials to add
     */
    fun registerWithDigitalCredentialsAPI(entries: CredentialsContainer)

    /**
     * Retrieves request from the digital credentials browser API
     */
    fun getCurrentDCAPIData(): DCAPIRequest?

    /**
     * Prepares the credential response and sends it back to the invoking application
     */
    fun prepareDCAPICredentialResponse(responseJson: ByteArray, dcApiRequest: DCAPIRequest)

}

class DummyPlatformAdapter : PlatformAdapter {
    override fun openUrl(url: String) {
    }

    override fun writeToFile(text: String, fileName: String, folderName: String) {
    }

    override fun readFromFile(fileName: String, folderName: String): String? {
        return null
    }

    override fun clearFile(fileName: String, folderName: String) {
    }

    override fun shareLog() {
    }

    override fun registerWithDigitalCredentialsAPI(entries: CredentialsContainer) {
    }

    override fun getCurrentDCAPIData(): DCAPIRequest? {
        return null
    }

    override fun prepareDCAPICredentialResponse(responseJson: ByteArray, dcApiRequest: DCAPIRequest) {
    }

}

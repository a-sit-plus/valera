package at.asitplus.wallet.app.common

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.josef.JsonWebKey
import at.asitplus.signum.indispensable.josef.JsonWebKeySet
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_update_action
import at.asitplus.valera.resources.snackbar_update_hint
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialList
import at.asitplus.wallet.app.common.dcapi.DCAPIExportService
import at.asitplus.wallet.app.common.dcapi.data.DCAPIRequest
import at.asitplus.wallet.app.common.dcapi.data.preview.PreviewDCAPIRequest
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.MediaTypes
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusListTokenPayload
import at.asitplus.wallet.lib.jws.VerifyJwsObject
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import at.asitplus.wallet.lib.rqes.Initializer.initRqesModule
import data.storage.AntilogAdapter
import data.storage.DataStoreService
import data.storage.PersistentSubjectCredentialStore
import getImageDecoder
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.swiftzer.semver.SemVer
import org.jetbrains.compose.resources.getString
import org.multipaz.prompt.PromptModel
import ui.navigation.IntentService

/**
 * Main class to hold all services needed in the Compose App.
 */
class WalletMain(
    val keyMaterial: WalletKeyMaterial,
    private val dataStoreService: DataStoreService,
    val platformAdapter: PlatformAdapter,
    var subjectCredentialStore: PersistentSubjectCredentialStore =
        PersistentSubjectCredentialStore(dataStoreService),
    val buildContext: BuildContext,
    promptModel: PromptModel
) {
    lateinit var walletConfig: WalletConfig
    lateinit var credentialValidator: Validator
    lateinit var holderAgent: HolderAgent
    lateinit var provisioningService: ProvisioningService
    lateinit var httpService: HttpService
    lateinit var presentationService: PresentationService
    lateinit var signingService: SigningService
    lateinit var dcApiExportService: DCAPIExportService
    lateinit var intentService: IntentService
    val errorService = ErrorService()
    val snackbarService = SnackbarService()
    private val regex = Regex("^(?=\\[[0-9]{2})", option = RegexOption.MULTILINE)
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
        errorService.emit(error)
    }
    val scope = CoroutineScope(Dispatchers.Default + coroutineExceptionHandler + promptModel)

    init {
        at.asitplus.wallet.mdl.Initializer.initWithVCK()
        at.asitplus.wallet.idaustria.Initializer.initWithVCK()
        at.asitplus.wallet.eupid.Initializer.initWithVCK()
        at.asitplus.wallet.eupidsdjwt.Initializer.initWithVCK()
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
    fun initialize() {
        walletConfig = WalletConfig(dataStoreService = this.dataStoreService, errorService = errorService)
        subjectCredentialStore = PersistentSubjectCredentialStore(dataStoreService)

        httpService = HttpService(buildContext)
        credentialValidator = Validator(
            resolveStatusListToken = {
                val httpResponse = httpService.buildHttpClient().get(it.string) {
                    headers.set(HttpHeaders.Accept, MediaTypes.Application.STATUSLIST_JWT)
                }
                StatusListToken.StatusListJwt(
                    JwsSigned.deserialize<StatusListTokenPayload>(
                        StatusListTokenPayload.serializer(),
                        httpResponse.bodyAsText()
                    ).getOrThrow(),
                    resolvedAt = Clock.System.now(),
                )
            },
            verifyJwsObject = VerifyJwsObject(publicKeyLookup = issuerKeyLookup())
        )
        holderAgent = HolderAgent(
            keyMaterial = keyMaterial,
            validator = credentialValidator,
            subjectCredentialStore = subjectCredentialStore,
        )
        intentService = IntentService(
            platformAdapter
        )
        httpService = HttpService(buildContext)
        provisioningService = ProvisioningService(
            intentService,
            dataStoreService,
            keyMaterial,
            holderAgent,
            walletConfig,
            errorService,
            httpService
        )
        presentationService = PresentationService(
            platformAdapter,
            keyMaterial,
            holderAgent,
            httpService,
        )
        signingService = SigningService(
            intentService,
            dataStoreService,
            errorService,
            snackbarService,
            httpService
        )

        this.dcApiExportService = DCAPIExportService(platformAdapter, scope)
        startListeningForNewCredentialsDCAPI()
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
        signingService.reset()

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

    private fun startListeningForNewCredentialsDCAPI() {
        try {
            Napier.d("DC API: Starting to observe credentials")
            subjectCredentialStore.observeStoreContainer().onEach { storeContainer ->
                dcApiExportService.registerCredentialWithSystem(storeContainer)
            }.launchIn(CoroutineScope(Dispatchers.IO))
        } catch (e: Throwable) {
            Napier.w("DC API: Could not update credentials with system", e)
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

    suspend fun checkRevocationStatus(storeEntry: SubjectCredentialStore.StoreEntry) = when (val it = storeEntry) {
        is SubjectCredentialStore.StoreEntry.Iso -> credentialValidator.checkRevocationStatus(it.issuerSigned)
        is SubjectCredentialStore.StoreEntry.SdJwt -> credentialValidator.checkRevocationStatus(it.sdJwt)
        is SubjectCredentialStore.StoreEntry.Vc -> credentialValidator.checkRevocationStatus(it.vc)
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
     * @param scope CoroutineScope for registering credentials
     */
    fun registerWithDigitalCredentialsAPI(entries: CredentialList, scope: CoroutineScope)

    /**
     * Retrieves request from the digital credentials browser API
     */
    fun getCurrentDCAPIData(): DCAPIRequest?

    /**
     * Prepares the credential response and sends it back to the invoking application
     */
    fun prepareDCAPICredentialResponse(responseJson: ByteArray, dcApiRequestPreview: PreviewDCAPIRequest)

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

    override fun registerWithDigitalCredentialsAPI(entries: CredentialList, scope: CoroutineScope) {
    }

    override fun getCurrentDCAPIData(): DCAPIRequest? {
        return null
    }

    override fun prepareDCAPICredentialResponse(
        responseJson: ByteArray,
        dcApiRequestPreview: PreviewDCAPIRequest
    ) {
    }

}

package at.asitplus.wallet.app.common

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eprescription.EPrescriptionScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.DefaultVerifierCryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.Parser
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.cbor.DefaultCoseService
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.jws.DefaultJwsService
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import data.storage.AntilogAdapter
import data.storage.DataStoreService
import data.storage.PersistentSubjectCredentialStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Main class to hold all services needed in the Compose App.
 */
class WalletMain(
    val cryptoService: WalletCryptoService,
    private val holderKeyService: HolderKeyService,
    private val dataStoreService: DataStoreService,
    val platformAdapter: PlatformAdapter,
    var subjectCredentialStore: PersistentSubjectCredentialStore = PersistentSubjectCredentialStore(dataStoreService),
    val buildContext: BuildContext,
    val errorService: ErrorService = ErrorService(mutableStateOf(false), mutableStateOf(null)),
    val scope: CoroutineScope,
) {
    lateinit var walletConfig: WalletConfig
    lateinit var holderAgent: HolderAgent
    lateinit var provisioningService: ProvisioningService
    lateinit var httpService: HttpService
    lateinit var presentationService: PresentationService
    lateinit var snackbarService: SnackbarService
    private val regex = Regex("^(?=\\[[0-9]{2})", option = RegexOption.MULTILINE)


    init {
        at.asitplus.wallet.mdl.Initializer.initWithVCK()
        at.asitplus.wallet.idaustria.Initializer.initWithVCK()
        at.asitplus.wallet.eupid.Initializer.initWithVCK()
        at.asitplus.wallet.cor.Initializer.initWithVCK()
        at.asitplus.wallet.por.Initializer.initWithVCK()
        at.asitplus.wallet.eprescription.Initializer.initWithVCK()
        Napier.takeLogarithm()
        Napier.base(AntilogAdapter(platformAdapter, ""))
    }

    val availableSchemes = listOf(
        MobileDrivingLicenceScheme,
        IdAustriaScheme,
        EuPidScheme,
        CertificateOfResidenceScheme,
        PowerOfRepresentationScheme,
        EPrescriptionScheme
    )

    @Throws(Throwable::class)
    fun initialize(snackbarService: SnackbarService) {
        walletConfig =
            WalletConfig(dataStoreService = this.dataStoreService, errorService = errorService)
        subjectCredentialStore = PersistentSubjectCredentialStore(dataStoreService)
        holderAgent = HolderAgent(
            validator = Validator(DefaultVerifierCryptoService(), Parser()),
            subjectCredentialStore = subjectCredentialStore,
            jwsService = DefaultJwsService(cryptoService),
            coseService = DefaultCoseService(cryptoService),
            keyPair = cryptoService.keyMaterial,
        )

        httpService = HttpService()
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
            httpService
        )
        this.snackbarService = snackbarService


    }

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
        credentialScheme: ConstantIndex.CredentialScheme,
        credentialRepresentation: ConstantIndex.CredentialRepresentation,
        requestedAttributes: Set<NormalizedJsonPath>?,
        onSuccess: () -> Unit,
    ) {
        scope.launch {
            try {
                provisioningService.startProvisioning(
                    host = host,
                    credentialScheme = credentialScheme,
                    credentialRepresentation = credentialRepresentation,
                    requestedAttributes = requestedAttributes,
                )
                onSuccess()
            } catch (e: Exception) {
                errorService.emit(e)
            }
        }
    }
}

/**
 * Interface which defines native keychain callbacks for the ID Holder
 */
interface HolderKeyService {
    /**
     * Clears the private and public key from the keychain/keystore
     */
    fun clear()
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
    fun decodeImage(image: ByteArray): ImageBitmap

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
     * Exits the app in the event of an uncorrectable error
     */
    fun exitApp()

    /**
     * Opens the platform specific share dialog
     */
    fun shareLog()
}

class DummyPlatformAdapter : PlatformAdapter {
    override fun openUrl(url: String) {
    }

    override fun decodeImage(image: ByteArray): ImageBitmap {
        return ImageBitmap(1, 1)
    }

    override fun writeToFile(text: String, fileName: String, folderName: String) {
    }

    override fun readFromFile(fileName: String, folderName: String): String? {
        return null
    }

    override fun clearFile(fileName: String, folderName: String) {
    }

    override fun exitApp() {
    }

    override fun shareLog() {
    }

}

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.KmmResult
import at.asitplus.iso.EncryptionParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.*
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import at.asitplus.wallet.app.common.di.appModule
import data.storage.RealDataStoreService
import data.storage.createDataStore
import io.github.aakira.napier.Napier
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.prompt.IosPromptModel
import platform.AVFoundation.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import ui.theme.darkScheme
import ui.theme.lightScheme

actual fun getPlatformName(): String = "iOS"

@Composable
actual fun getColorScheme(): ColorScheme {
    return if (isSystemInDarkTheme()) {
        darkScheme
    } else {
        lightScheme
    }
}

@ExperimentalMaterial3Api
fun MainViewController(
    buildContext: BuildContext,
): UIViewController {
    val iosPlatformAdapter = IosPlatformAdapter()
    val dataStoreService = RealDataStoreService(createDataStore(), iosPlatformAdapter)
    val keystoreService = KeystoreService(dataStoreService)
    val promptModel = IosPromptModel()
    val walletDependencyProvider = WalletDependencyProvider(
        keystoreService,
        dataStoreService,
        iosPlatformAdapter,
        buildContext = buildContext,
        promptModel = promptModel
    )
    val capabilitiesModule = module {
        scope(named(SESSION_NAME)) {
            scopedOf(::RealCapabilitiesService) binds arrayOf(CapabilitiesService::class)
        }
    }
    val module = appModule(walletDependencyProvider, capabilitiesModule)

    return ComposeUIViewController {
        PromptDialogs(promptModel)
        App(module)
    }
}

class IosPlatformAdapter(
) : PlatformAdapter {
    override fun openUrl(url: String) {
        val url = NSURL(string = url)
        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url, mapOf<Any?, Any?>(), null)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun writeToFile(text: String, fileName: String, folderName: String) {
        val baseUrl = getBaseUrl() ?: return

        val folderUrl = baseUrl.URLByAppendingPathComponent(folderName)
        val folderPath = folderUrl?.path ?: return

        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(folderPath)) {
            fileManager.createDirectoryAtPath(
                path = folderPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }

        val fileUrl = folderUrl.URLByAppendingPathComponent(fileName)
        val filePath = fileUrl?.path ?: return

        val data = text.encodeToByteArray().toNSData()

        if (fileManager.fileExistsAtPath(filePath)) {
            val handle = NSFileHandle.fileHandleForWritingAtPath(filePath)
            handle?.seekToEndOfFile()
            handle?.writeData(data)
            handle?.closeFile()
        } else {
            fileManager.createFileAtPath(
                path = filePath,
                contents = data,
                attributes = null
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun readFromFile(fileName: String, folderName: String): String? {
        val baseUrl = getBaseUrl() ?: return null

        val folderUrl = baseUrl.URLByAppendingPathComponent(folderName)
        val folderPath = folderUrl?.path ?: return null

        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(folderPath)) {
            fileManager.createDirectoryAtPath(
                path = folderPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }

        val fileUrl = folderUrl.URLByAppendingPathComponent(fileName)
        val filePath = fileUrl?.path ?: return null

        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val content = NSString.stringWithContentsOfFile(
                path = filePath,
                encoding = NSUTF8StringEncoding,
                error = errorPtr.ptr
            )
            errorPtr.value?.let {
                Napier.e("Unable to read file: $fileName")
            }
            return content
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun clearFile(fileName: String, folderName: String) {
        val baseUrl = getBaseUrl() ?: return

        val folderUrl = baseUrl.URLByAppendingPathComponent(folderName)
        val folderPath = folderUrl?.path ?: return

        val fileManager = NSFileManager.defaultManager

        if (!fileManager.fileExistsAtPath(folderPath)) {
            fileManager.createDirectoryAtPath(
                path = folderPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }

        val fileUrl = folderUrl.URLByAppendingPathComponent(fileName)
        val filePath = fileUrl?.path ?: return

        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            fileManager.removeItemAtPath(
                path = filePath,
                error = errorPtr.ptr
            )
            errorPtr.value?.let {
                Napier.e("Unable to clear file: $fileName")
            }
        }
    }

    override fun shareLog() {
        val baseUrl = getBaseUrl() ?: return

        val folderUrl = baseUrl.URLByAppendingPathComponent("logs")
        val fileUrl = folderUrl?.URLByAppendingPathComponent("log.txt") ?: return

        dispatch_async(dispatch_get_main_queue()) {
            val connectedScenes = UIApplication.sharedApplication.connectedScenes
            val windowScene = (connectedScenes as NSSet)
                .anyObject() as? UIWindowScene
            val currentController = windowScene
                ?.windows
                ?.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true }
                ?.let { (it as UIWindow).rootViewController() }

            val activityVC = UIActivityViewController(
                activityItems = listOf(fileUrl),
                applicationActivities = null
            )

            currentController?.presentViewController(
                activityVC,
                animated = true,
                completion = null
            )
        }
    }

    override fun registerWithDigitalCredentialsAPI(
        entries: CredentialRegistry,
        scope: CoroutineScope
    ) {
        //TODO("Not yet implemented")
    }

    override fun getCurrentDCAPIVerificationData(): KmmResult<RequestParametersFrom.DcApiRequest> {
        TODO("Not yet implemented")
    }

    override fun prepareDCAPIIsoMdocCredentialResponse(
        responseJson: ByteArray,
        sessionTranscript: ByteArray,
        encryptionParameters: EncryptionParameters
    ) {
        //TODO("Not yet implemented")
    }

    override fun prepareDCAPIOid4vpCredentialResponse(responseJson: String, success: Boolean) {
        // TODO("Not yet implemented")
    }

    override fun openDeviceSettings() {
        openUrl(UIApplicationOpenSettingsURLString)
    }

    fun getBaseUrl(): NSURL? {
        val urls = NSFileManager.defaultManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask
        )
        return urls.first() as? NSURL
    }

    override fun getCameraPermission(): Boolean? {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)

        return when (status) {
            AVAuthorizationStatusAuthorized -> true
            AVAuthorizationStatusNotDetermined -> null
            AVAuthorizationStatusDenied -> false
            AVAuthorizationStatusRestricted -> false
            else -> null
        }
    }

}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData = memScoped {
    this@toNSData.usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = this@toNSData.size.toULong()
        )
    }
}

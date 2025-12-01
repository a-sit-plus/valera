import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.KmmResult
import at.asitplus.dcapi.request.DCAPIRequest
import at.asitplus.iso.EncryptionParameters
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import data.storage.RealDataStoreService
import data.storage.createDataStore
import io.github.aakira.napier.Napier
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.prompt.IosPromptModel
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import ui.theme.darkScheme
import ui.theme.lightScheme
import at.asitplus.wallet.app.ios.DigitalCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import platform.UIKit.UIViewController
import platform.Foundation.NSData
import kotlin.experimental.ExperimentalNativeApi


actual fun getPlatformName(): String = "iOS"

@Composable
actual fun getColorScheme(): ColorScheme {
    return if (isSystemInDarkTheme()) {
        darkScheme
    } else {
        lightScheme
    }
}



@OptIn(ExperimentalNativeApi::class)
fun initLogger(isDebug: Boolean) {
    setUnhandledExceptionHook { throwable ->
        val msg = throwable.message ?: throwable.toString()
        Napier.e(
            message = "UNCAUGHT: $msg",
        )
    }
    /*Napier.base(
        when {
            isDebug -> OsLogAntilog()        // use OSLog in debug too if you want
            else -> OsLogAntilog()
        }
    )*/
}

fun MainViewController(
    buildContext: BuildContext,
): UIViewController {
    initLogger(true)
    val iosPlatformAdapter = IosPlatformAdapter()
    val dataStoreService = RealDataStoreService(createDataStore(), iosPlatformAdapter)
    val keystoreService = KeystoreService(dataStoreService)
    val promptModel = IosPromptModel()

    return ComposeUIViewController {
        PromptDialogs(promptModel)
        App(
            WalletDependencyProvider(
                keystoreService,
                dataStoreService,
                iosPlatformAdapter,
                buildContext = buildContext,
                promptModel = promptModel
            )
        )
    }
}

/*@Composable
private fun ComposeApp() { // This function also may be placed in commonMain source set.
    MaterialTheme {
        Surface {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .background(Color.Green.copy(alpha = 0.3f))
            ) {
                Text("top", Modifier.align(Alignment.TopCenter))
                Text("ComposeApp", Modifier.align(Alignment.Center))
                Text("bottom", Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}*/

/*fun ComposeEntryPoint(): UIViewController =
    try {
        ComposeUIViewController {
            ComposeApp()
        }
    } catch (t: Throwable) {
        Napier.e("Failed to construct ComposeUIViewController (no-arg)", t)
        fallbackViewController("An unexpected error occurred.")
    }*/

// Session information and callbacks bridged from the iOS ISO18013 scene
data class MdocRequestSession(
    val requestingWebsiteOrigin: String?,
    val requestPayload: NSData?,
    val requestDescription: String?,
    val onSendResponse: (NSData) -> Unit,
    val onCancel: () -> Unit
)

// Overload that accepts the bridged context so Kotlin can read and act
/*fun ComposeEntryPoint(
    requestingWebsiteOrigin: String?,
    requestPayload: NSData?,
    requestDescription: String?,
    onSendResponse: (NSData) -> Unit,
    onCancel: () -> Unit
): UIViewController = try {
    ComposeUIViewController {
        val session = MdocRequestSession(
            requestingWebsiteOrigin = requestingWebsiteOrigin,
            requestPayload = requestPayload,
            requestDescription = requestDescription,
            onSendResponse = onSendResponse,
            onCancel = onCancel
        )

        ComposeApp(session)
    }
} catch (t: Throwable) {
    Napier.e("Failed to construct ComposeUIViewController (with session)", t)
    fallbackViewController("Unable to open request UI.")
}*/

// Nullable factories so Swift can decide how to render fallback UI
/*fun tryComposeEntryPoint(): UIViewController? = try {
    ComposeUIViewController { ComposeApp() }
} catch (t: Throwable) {
    Napier.e("ComposeEntryPoint() failed", t)
    null
}*/

fun tryComposeEntryPoint(
    requestingWebsiteOrigin: String?,
    requestPayload: NSData?,
    requestDescription: String?,
    onSendResponse: (NSData) -> Unit,
    onCancel: () -> Unit
): UIViewController? = try {
    initLogger(true)
    ComposeUIViewController {
        val session = MdocRequestSession(
            requestingWebsiteOrigin = requestingWebsiteOrigin,
            requestPayload = requestPayload,
            requestDescription = requestDescription,
            onSendResponse = onSendResponse,
            onCancel = onCancel
        )
        ComposeApp(session)
    }
} catch (t: Throwable) {
    Napier.e("ComposeEntryPoint(session) failed", t)
    null
}

/*private fun fallbackViewController(message: String): UIViewController {
    val vc = UIViewController()
    val label = UILabel().apply {
        text = message
        textAlignment = NSTextAlignmentCenter
        // 0 means no limit
        numberOfLines = 0L
        textColor = UIColor.labelColor
    }
    vc.view.backgroundColor = UIColor.systemBackgroundColor
    vc.view.addSubview(label)
    label.translatesAutoresizingMaskIntoConstraints = false
    val constraints = listOf(
        label.centerXAnchor.constraintEqualToAnchor(vc.view.centerXAnchor),
        label.centerYAnchor.constraintEqualToAnchor(vc.view.centerYAnchor),
        label.leadingAnchor.constraintGreaterThanOrEqualToAnchor(vc.view.leadingAnchor, 20.0),
        vc.view.trailingAnchor.constraintGreaterThanOrEqualToAnchor(label.trailingAnchor, 20.0)
    )
    constraints.forEach { it.active = true }
    return vc
}*/

@Composable
private fun ComposeApp(session: MdocRequestSession?) {
    initLogger(true)
    // Minimal surface to demonstrate reading values and invoking callbacks
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .background(Color(0xFF101010))
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ISO 18013 Request", color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("Origin: ${session?.requestingWebsiteOrigin ?: "-"}", color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Request: ${session?.requestDescription ?: (session?.requestPayload?.let { "${it.length} bytes" } ?: "-")}",
                        color = Color.White
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Send a dummy response for now; host app should provide real CBOR/bytes payload from Kotlin logic
                        Button(onClick = {
                            // For demo, echo back an empty NSData; real implementation should construct correct payload
                            session?.onSendResponse(NSData())
                        }) {
                            Text("Send Response")
                        }
                        Button(onClick = { session?.onCancel?.invoke() }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

fun dcapiViewController(
    buildContext: BuildContext,
): UIViewController {
    /*val iosPlatformAdapter = IosPlatformAdapter()
    val dataStoreService = RealDataStoreService(createDataStore(), iosPlatformAdapter)
    val keystoreService = KeystoreService(dataStoreService)
    val promptModel = IosPromptModel()*/
    initLogger(true)
    return ComposeUIViewController {

        /*App(
            WalletDependencyProvider(
                keystoreService,
                dataStoreService,
                iosPlatformAdapter,
                buildContext = buildContext,
                promptModel = promptModel,
                shouldListen = false
            )
        )*/
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
        scope.launch(Dispatchers.Default) {
            for (entry in entries.credentials) {
                val id = entry.isoEntry?.id ?: entry.sdJwtEntry?.jwtId
                val docType = entry.isoEntry?.docType ?: entry.sdJwtEntry?.verifiableCredentialType
                if (id != null && docType != null) {
                    storeDocumentFromSwift(id, docType)
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun storeDocumentFromSwift(id: String, docType: String): Boolean = suspendCancellableCoroutine { cont ->
        try {
            Napier.d("storeDocumentFromSwift invoked")
            // TODO check if doctype is supported, otherwise don't add it
            DigitalCredentials.storeDocumentWithId(id, docType) { success ->
                Napier.d("storeDocumentFromSwift callback with $success")
                if (cont.isActive) cont.resume(success)
            }
            Napier.d("storeDocumentFromSwift got back from swift")

        } catch (e: Throwable) {
            Napier.e("Error while invoking Swift code", e)
        }

    }

    override fun getCurrentDCAPIData(): KmmResult<DCAPIRequest> {
        return KmmResult.failure(Throwable("Using Swift platform adapter"))
    }

    override fun prepareDCAPIIsoMdocCredentialResponse(
        responseJson: ByteArray,
        sessionTranscript: ByteArray,
        encryptionParameters: EncryptionParameters
    ) {
        //TODO("Not yet implemented")
    }

    override fun prepareDCAPIOid4vpCredentialResponse(responseJson: String, success: Boolean) {
        //TODO("Not yet implemented")
    }

    fun getBaseUrl(): NSURL? {
        val urls = NSFileManager.defaultManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask
        )
        return urls.first() as? NSURL
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
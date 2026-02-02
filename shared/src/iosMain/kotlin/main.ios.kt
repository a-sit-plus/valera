import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.catching
import at.asitplus.KmmResult
import at.asitplus.dcapi.EncryptedResponse
import at.asitplus.dcapi.request.DCAPIWalletRequest
import at.asitplus.dcapi.request.IsoMdocRequest
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.CapabilitiesService
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.RealCapabilitiesService
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.dcapi.DCAPICreationRequest
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import at.asitplus.wallet.app.common.di.appModule
import at.asitplus.wallet.app.dcapi.IosDCAPIInvocationData
import at.asitplus.wallet.app.ios.DigitalCredentials
import data.storage.RealDataStoreService
import data.storage.createDataStore
import io.github.aakira.napier.Napier
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
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
import ui.navigation.IOS_DC_API_CALL
import ui.theme.darkScheme
import ui.theme.lightScheme
import kotlin.coroutines.resume
import kotlin.experimental.ExperimentalNativeApi


actual fun getPlatformName(): String = "iOS"

private val SUPPORTED_DOC_TYPES = listOf(
    "eu.europa.ec.av.1",
    "eu.europa.ec.eudi.pid.1",
    "org.iso.23220.photoid.1",
    "org.iso.23220.1.jp.mnc",
    "org.iso.18013.5.1.mDL"
)

@Composable
actual fun getColorScheme(): ColorScheme {
    return if (isSystemInDarkTheme()) {
        darkScheme
    } else {
        lightScheme
    }
}

private val iosIntentState = IntentState()

object MdocSessionManager {
    // TODO check if correct credentials are shown without credentialId set (and check behaviour on Android, should only show the one credential selected by the user)
    fun setSession(data: IosDCAPIInvocationData) {
        iosIntentState.dcapiInvocationData.value = data
        iosIntentState.appLink.value = IOS_DC_API_CALL
        Napier.d("MdocSessionManager: Session set with request of length ${data.rawRequest?.length} from origin ${data.origin}")
    }

    fun clearSession() {
        iosIntentState.dcapiInvocationData.value = null
        iosIntentState.appLink.value = null
        Napier.d("MdocSessionManager: Session cleared")
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
    val iosPlatformAdapter = IosPlatformAdapter(iosIntentState)
    val dataStoreService = RealDataStoreService(createDataStore(), iosPlatformAdapter)
    val keystoreService = KeystoreService(dataStoreService)
    val promptModel = IosPromptModel.Builder().apply { addCommonDialogs() }.build()
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
        App(
            koinModule = module,
            intentState = iosIntentState
        )
    }
}

// Session information and callbacks bridged from the iOS ISO18013 scene
data class MdocRequestSession(
    val requestingWebsiteOrigin: String?,
    val requestPayload: NSData?,
    val requestDescription: String?,
    val onSendResponse: (NSData) -> Unit,
    val onCancel: () -> Unit
)

@Composable
private fun ComposeApp(session: MdocRequestSession?) {
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

@Composable
private fun MdocRequestUI(
    requestingWebsiteOrigin: String?,
    requestedElements: List<String>,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Presentation Request",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                requestingWebsiteOrigin?.let {
                    Text("Request from:")
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text("Requested Data:")
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    requestedElements.forEach { element ->
                        Text("- $element")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Button(onClick = onAccept) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}

fun MdocRequestViewController(
    requestingWebsiteOrigin: String?,
    requestedElements: List<String>,
    onSendResponse: (NSData) -> Unit,
    onCancel: () -> Unit
): UIViewController {

    return ComposeUIViewController {
        MdocRequestUI(
            requestingWebsiteOrigin = requestingWebsiteOrigin,
            requestedElements = requestedElements,
            onAccept = {
                Napier.d("MdocRequestUI onAccept")
                // Create a dummy response for now
                val dummyResponse = "dummy response data".encodeToByteArray().toNSData()
                onSendResponse(dummyResponse)
            },
            onCancel = onCancel
        )
    }
}

class IosPlatformAdapter(
    private val intentState: IntentState
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
            if (docType !in SUPPORTED_DOC_TYPES) {
                Napier.w("DocType '$docType' is not supported on iOS, will not add it to Wallet")
                if (cont.isActive) cont.resume(false)
                return@suspendCancellableCoroutine
            }
            DigitalCredentials.storeDocumentWithId(id, docType) { success ->
                Napier.d("storeDocumentFromSwift callback with $success")
                if (cont.isActive) cont.resume(success)
            }
            Napier.d("storeDocumentFromSwift got back from swift")

        } catch (e: Throwable) {
            Napier.e("Error while invoking Swift code", e)
        }

    }

    override fun getCurrentDCAPIVerificationData(): KmmResult<DCAPIWalletRequest> {
        Napier.d("getCurrentDCAPIData called")
        // TODO update code so that getCurrentDCAPIData is invoked, i.e. open the right views
        return (intentState.dcapiInvocationData.value as IosDCAPIInvocationData?)?.let {
            try {
                val isoMdocRequest = it.rawRequest?.let { request -> Json.decodeFromString<IsoMdocRequest>(request) }
                    ?: throw IllegalStateException("No request data available")
                //TODO check added nullability of DCAPIWalletRequest in Android code and terminal sp
                val walletRequest = DCAPIWalletRequest.IsoMdoc(
                    isoMdocRequest = isoMdocRequest,
                    callingOrigin = it.origin ?: throw IllegalStateException("No origin received"),
                )
                KmmResult.success(walletRequest)
            } catch (e: Throwable) {
                Napier.e("Error parsing mdoc request", e)
                KmmResult.failure(e)
            }
        } ?: KmmResult.failure(Throwable("No request data available"))
    }

    override fun getCurrentDCAPICreationData(): KmmResult<DCAPICreationRequest> = catching {
        throw IllegalStateException("Not supported on iOS")
    }

    override fun prepareDCAPICredentialResponse(response: String, success: Boolean) {
        Napier.w("Got error response: $response")
        //TODO is there a way to convey error responses via ISO18013-7?
        // send empty response for now
        (intentState.dcapiInvocationData.value as IosDCAPIInvocationData?)?.let { (_, _, sendCredentialResponseToInvoker) ->
            sendCredentialResponseToInvoker.invoke(ByteArray(0).toNSData())
            MdocSessionManager.clearSession()
        } ?: throw IllegalStateException("Callback for response not found")
    }

    override fun prepareIsoMdocDCAPICredentialResponse(response: EncryptedResponse, success: Boolean) =
        (intentState.dcapiInvocationData.value as IosDCAPIInvocationData?)?.let { (_, _, sendCredentialResponseToInvoker) ->
            Napier.d("prepareDCAPICredentialResponse called with $response")
            val encodedResponse = coseCompliantSerializer.encodeToByteArray(response)
            Napier.d("encodedResponse: ${encodedResponse.toHexString()}")
            sendCredentialResponseToInvoker.invoke(encodedResponse.toNSData())
            MdocSessionManager.clearSession()
        } ?: throw IllegalStateException("Callback for response not found")

    override fun prepareDCAPICreationResponse(response: String, success: Boolean) {
        Napier.w("DC API issuing not supported on iOS")
    }

    override fun hasPendingDCAPICreationRequest(): Boolean {
        Napier.w("DC API issuing not supported on iOS")
        return false
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

    override fun finishApp() {
        // No-op on iOS; hosting app controls dismissal.
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

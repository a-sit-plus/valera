import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.catching
import at.asitplus.KmmResult
import at.asitplus.dcapi.DigitalCredentialInterface
import at.asitplus.dcapi.toIosIsoMdocResponseBytes
import at.asitplus.dcapi.ios.IosDcApiMdocPreRequestSummary
import at.asitplus.dcapi.request.IsoMdocRequest
import at.asitplus.dcapi.request.toRequestParametersFrom
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.dcapi.DCAPIIssuingRequest
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialRepresentation
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialType
import at.asitplus.wallet.app.common.dcapi.DCAPIExportService
import at.asitplus.wallet.app.common.dcapi.DCAPIVerificationData
import at.asitplus.wallet.app.common.AV_DOC_TYPE
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import at.asitplus.wallet.app.dcapi.IosDCAPIInvocationData
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_digital_credentials_store_failed
import io.github.aakira.napier.Napier
import at.asitplus.wallet.app.ios.DigitalCredentials
import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.mdl.MDL_DOCTYPE
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import org.multipaz.compose.prompt.PromptDialogs
import platform.AVFoundation.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import ui.theme.darkScheme
import ui.theme.lightScheme
import kotlin.collections.mapValues


actual fun getPlatformName(): String = "iOS"

internal val IOS_SUPPORTED_DOC_TYPES = setOf(
    AV_DOC_TYPE,
    EU_PID_DOCTYPE,
    "org.iso.23220.photoid.1",
    MDL_DOCTYPE
)

internal fun iosIssuingDocumentId(docType: String): String =
    "${DCAPIExportService.ISSUING_CREDENTIAL_ID}:mdoc:${docType.encodeToByteArray().encodeHex()}"

private fun ByteArray.encodeHex(): String = joinToString(separator = "") { byte ->
    byte.toUByte().toString(radix = 16).padStart(length = 2, padChar = '0')
}

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
    val (intentState, sessionService, promptModel) = getOrCreateIosSession(buildContext)

    return ComposeUIViewController {
        PromptDialogs(promptModel)
        App(
            sessionService = sessionService,
            intentState = intentState
        )
    }
}

@ExperimentalMaterial3Api
fun TransientFlowMainViewController(
    buildContext: BuildContext,
    openUrl: ((String) -> Unit)? = null,
): UIViewController {
    val (intentState, sessionService, promptModel) = getOrCreateIosTransientFlowSession(buildContext, openUrl)

    return ComposeUIViewController {
        PromptDialogs(promptModel)
        TransientFlowApp(
            sessionService = sessionService,
            intentState = intentState
        )
    }
}

class IosPlatformAdapter(
    private val intentState: IntentState,
    private val openUrlHandler: ((String) -> Unit)? = null,
) : PlatformAdapter {
    override val dcApiVerificationIssuanceTypes: Set<DCAPICredentialType> =
        IOS_SUPPORTED_DOC_TYPES.mapTo(mutableSetOf()) {
            DCAPICredentialType(DCAPICredentialRepresentation.ISO_MDOC, it)
        }
    private companion object {
        const val REGISTERED_DOCUMENT_IDS_DEFAULTS_KEY = "dcapi.registeredDocumentIds"
        const val ISO_DC_API_VALIDATION_ERROR = "ISO 18013-7 Annex C request validation failed"
        val registeredDocumentIdsLock = Mutex()
        val registeredDocumentIds = mutableSetOf<String>().apply {
            addAll(loadRegisteredDocumentIds())
        }

        fun loadRegisteredDocumentIds(): Set<String> =
            NSUserDefaults.standardUserDefaults
                .stringArrayForKey(REGISTERED_DOCUMENT_IDS_DEFAULTS_KEY)
                ?.filterIsInstance<String>()
                ?.toSet()
                ?: emptySet()

        fun saveRegisteredDocumentIds(ids: Set<String>) {
            NSUserDefaults.standardUserDefaults.setObject(
                ids.toList(),
                forKey = REGISTERED_DOCUMENT_IDS_DEFAULTS_KEY
            )
        }
    }

    override fun openUrl(url: String) {
        dispatch_async(dispatch_get_main_queue()) {
            openUrlHandler?.let {
                it(url)
                return@dispatch_async
            }
            val url = NSURL(string = url)
            if (UIApplication.sharedApplication.canOpenURL(url)) {
                UIApplication.sharedApplication.openURL(url, mapOf<Any?, Any?>(), null)
            }
        }
    }

    override fun tryOpenCrossDeviceQrCode(uri: String): Boolean = false

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

    @OptIn(ExperimentalForeignApi::class)
    override fun shareLog() {
        val baseUrl = getBaseUrl() ?: return

        val folderUrl = baseUrl.URLByAppendingPathComponent("logs")
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

        val fileUrl = folderUrl?.URLByAppendingPathComponent("log.txt") ?: return
        val filePath = fileUrl.path ?: return
        if (!fileManager.fileExistsAtPath(filePath)) {
            fileManager.createFileAtPath(
                path = filePath,
                contents = NSData(),
                attributes = null
            )
        }

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

    override suspend fun registerWithDigitalCredentialsAPI(
        entries: CredentialRegistry,
        scope: CoroutineScope
    ) {
        withContext(Dispatchers.Default) {
            // Serialize the whole removal+add process so concurrent snapshots can't interleave.
            registeredDocumentIdsLock.withLock {
                val storedDocuments = entries.credentials.mapNotNull { entry ->
                    val id = entry.isoEntry?.id ?: entry.sdJwtEntry?.jwtId
                    val docType = entry.isoEntry?.docType ?: entry.sdJwtEntry?.verifiableCredentialType
                    if (id != null && docType in IOS_SUPPORTED_DOC_TYPES) id to docType else null
                }
                val storedDocTypes = storedDocuments.mapTo(mutableSetOf()) { it.second }
                val issuingDocuments = IOS_SUPPORTED_DOC_TYPES
                    .filterNot { it in storedDocTypes }
                    .map { docType -> iosIssuingDocumentId(docType) to docType }
                val documents = storedDocuments + issuingDocuments
                val currentIds = documents.mapTo(mutableSetOf()) { it.first }
                val staleIds = registeredDocumentIds - currentIds
                staleIds.forEach { id ->
                    if (removeDocumentFromSwift(id, scope)) {
                        registeredDocumentIds.remove(id)
                        saveRegisteredDocumentIds(registeredDocumentIds)
                    }
                }
                for ((id, docType) in documents) {
                    // Only register credentials we haven't registered before.
                    if (id !in registeredDocumentIds) {
                        if (storeDocumentFromSwift(id, docType, scope)) {
                            registeredDocumentIds.add(id)
                            saveRegisteredDocumentIds(registeredDocumentIds)
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun storeDocumentFromSwift(
        id: String,
        docType: String,
        scope: CoroutineScope
    ): Boolean = suspendCancellableCoroutine { cont ->
        try {
            Napier.d("storeDocumentFromSwift invoked")
            if (docType !in IOS_SUPPORTED_DOC_TYPES) {
                Napier.w("DocType '$docType' is not supported on iOS, will not add it to the system")
                if (cont.isActive) cont.resume(false)
                return@suspendCancellableCoroutine
            }
            DigitalCredentials.storeDocumentWithId(id, docType) { errorMessage ->
                val success = errorMessage == null
                Napier.d("storeDocumentFromSwift callback with success=$success error=$errorMessage")
                if (!success) {
                    scope.launch {
                        val baseMessage = getString(Res.string.snackbar_digital_credentials_store_failed)
                        val details = errorMessage.takeIf { it.isNotBlank() }
                        IosSessionBridge.showSnackbar(
                            listOfNotNull(baseMessage, details)
                                .joinToString(": "),
                            SnackbarDuration.Long
                        )
                    }
                }
                if (cont.isActive) cont.resume(success)
            }
            Napier.d("storeDocumentFromSwift got back from swift")
        } catch (e: Throwable) {
            Napier.e("Error while invoking Swift code", e)
            if (cont.isActive) cont.resume(false)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun removeDocumentFromSwift(
        id: String,
        scope: CoroutineScope
    ): Boolean = suspendCancellableCoroutine { cont ->
        try {
            Napier.d("removeDocumentFromSwift invoked")
            DigitalCredentials.removeDocumentWithId(id) { errorMessage ->
                val success = errorMessage == null
                Napier.d("removeDocumentFromSwift callback with success=$success error=$errorMessage")
                if (!success) {
                    scope.launch {
                        IosSessionBridge.showSnackbar(
                            errorMessage.takeUnless { it.isNullOrBlank() } ?: "Unable to remove stale document registration",
                            SnackbarDuration.Long
                        )
                    }
                }
                if (cont.isActive) cont.resume(success)
            }
            Napier.d("removeDocumentFromSwift got back from swift")
        } catch (e: Throwable) {
            Napier.e("Error while invoking Swift code", e)
            if (cont.isActive) cont.resume(false)
        }
    }

    override fun getCurrentDCAPIVerificationData(): KmmResult<DCAPIVerificationData> {
        Napier.d("getCurrentDCAPIVerificationData called")
        intentState.pendingDCAPIVerificationIssuanceQueue.value.firstOrNull()?.let {
            return KmmResult.success(DCAPIVerificationData.IssuanceRequired(it))
        }
        return (intentState.dcapiInvocationData.value as IosDCAPIInvocationData?)?.let {
            try {
                val isoMdocRequest = it.rawRequest?.let { request -> Json.decodeFromString<IsoMdocRequest>(request) }
                    ?: throw IllegalStateException("No request data available")
                Napier.d("getCurrentDCAPIVerificationData: rawRequest docTypes=${isoMdocRequest.deviceRequest.docRequests.map { req -> req.itemsRequest.value.docType }}")
                Napier.d("getCurrentDCAPIVerificationData: rawRequest namespaces=${isoMdocRequest.deviceRequest.docRequests.map { req -> req.itemsRequest.value.namespaces.mapValues { (_, items) -> items.entries.map { item -> "${item.dataElementIdentifier}→retain=${item.intentToRetain}" } } }}")

                val parsedRequestSummary = it.parsedRequestSummary?.let { summary ->
                    Json.decodeFromString<IosDcApiMdocPreRequestSummary>(summary)
                } ?: throw IllegalStateException("No parsed request summary available")
                Napier.d("getCurrentDCAPIVerificationData: parsedSummary docTypes=${parsedRequestSummary.documentRequests.map { req -> req.docType }}")
                Napier.d("getCurrentDCAPIVerificationData: parsedSummary namespaces=${parsedRequestSummary.documentRequests.map { req -> req.namespaces.mapValues { (_, elems) -> elems.map { (id, retain) -> "$id→retain=$retain" } } }}")
                require(parsedRequestSummary.isConsistentWith(isoMdocRequest)) {
                    "Parsed ISO18013 mobile document pre-request is inconsistent with rawRequest"
                }
                val walletRequest = isoMdocRequest.toRequestParametersFrom(
                    callingOrigin = it.origin ?: throw IllegalStateException("No origin received"),
                    credentialIds = null,
                )
                KmmResult.success(DCAPIVerificationData.Presentation(walletRequest))
            } catch (e: Throwable) {
                Napier.e("Error parsing mdoc request", e)
                KmmResult.failure(e)
            }
        } ?: KmmResult.failure(Throwable("No request data available"))
    }

    override fun resolveCurrentDCAPIVerificationIssuance(
        credentialType: DCAPICredentialType,
        credentialId: String,
    ): KmmResult<Unit> = catching {
        require(credentialId.isNotBlank()) { "Issued credential has no DC API ID" }
        val queue = intentState.pendingDCAPIVerificationIssuanceQueue.value
        require(queue.firstOrNull() == credentialType) {
            "Issued credential type does not match the pending iOS request"
        }
        intentState.pendingDCAPIVerificationIssuanceQueue.value = queue.drop(1)
    }

    override fun getCurrentDCAPIIssuingData(): KmmResult<DCAPIIssuingRequest> = catching {
        throw IllegalStateException("Not supported by iOS")
    }

    override fun prepareDCAPICredentialResponse(response: DigitalCredentialInterface) {
        val invocation = (intentState.dcapiInvocationData.value as IosDCAPIInvocationData?)
            ?: throw IllegalStateException("Callback for response not found")
        try {
            Napier.d("prepareDCAPICredentialResponse called with $response")
            val encodedResponse = response.toIosIsoMdocResponseBytes()
            invocation.sendCredentialResponse.invoke(encodedResponse.toNSData())
        } catch (throwable: Throwable) {
            invocation.sendCredentialError(
                throwable.message ?: "Failed to build ISO 18013-7 Annex C response"
            )
            throw throwable
        } finally {
            IosSessionBridge.clearDcapiInvocation()
        }
    }

    override fun prepareDCAPICredentialError(error: String) {
        Napier.w("Got ISO 18013-7 Annex C validation error: $error")
        val invocation = (intentState.dcapiInvocationData.value as IosDCAPIInvocationData?)
            ?: throw IllegalStateException("Callback for response not found")
        try {
            // The common API carries an OAuth-style serialized error for Android. iOS uses
            // ISO 18013-7 Annex C, so keep that value diagnostic-only and fail the throwing
            // sendResponse closure with a protocol-neutral validation error.
            invocation.sendCredentialError(ISO_DC_API_VALIDATION_ERROR)
        } finally {
            IosSessionBridge.clearDcapiInvocation()
        }
    }

    override fun prepareDCAPIIssuingResponse(response: String, success: Boolean) {
        Napier.w("DC API issuing not supported by iOS")
    }

    override fun hasPendingDCAPIIssuingRequest(): Boolean {
        Napier.w("DC API issuing not supported by iOS")
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

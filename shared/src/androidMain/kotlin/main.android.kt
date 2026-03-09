import AndroidPlatformAdapter.Companion.openIdArrayKeys
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.credentials.CreateDigitalCredentialRequest
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.registry.provider.ClearCredentialRegistryRequest
import androidx.credentials.registry.provider.RegistryManager
import androidx.credentials.registry.provider.selectedEntryId
import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.dcapi.*
import at.asitplus.dcapi.request.verifier.DigitalCredentialRequestOptions
import at.asitplus.dcapi.request.verifier.toRequestParametersFrom
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.android.dcapi.AndroidDCAPIInvocationData
import at.asitplus.wallet.app.android.dcapi.CustomRegistry
import at.asitplus.wallet.app.android.security.RequestTrustAnchorsInitializer
import at.asitplus.wallet.app.common.*
import at.asitplus.wallet.app.common.dcapi.DCAPIIssuingRequest
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import at.asitplus.wallet.lib.openid.OpenId4VpHolder
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.prompt.PromptModel
import ui.theme.darkScheme
import ui.theme.lightScheme
import java.io.File
import java.security.MessageDigest


actual fun getPlatformName(): String = "Android"

private var requestTrustAnchorsInitializationAttempted = false
private val requestTrustAnchorsInitializer = RequestTrustAnchorsInitializer()


// Modified from https://developer.android.com/jetpack/compose/designsystems/material3
@Composable
actual fun getColorScheme(): ColorScheme {
    // Dynamic color is available on Android 12+, but let's use our color scheme for branding
    return if (isSystemInDarkTheme()) {
        darkScheme
    } else {
        lightScheme
    }
}

@ExperimentalMaterial3Api
@Composable
fun MainView(
    buildContext: BuildContext,
    promptModel: PromptModel,
    intentState: IntentState,
    sessionService: SessionService
) {
    val context = LocalContext.current

    if (!requestTrustAnchorsInitializationAttempted) {
        requestTrustAnchorsInitializer.initialize(context)
        requestTrustAnchorsInitializationAttempted = true
    }

    WalletRootView(
        buildContext = buildContext,
        promptModel = promptModel,
        intentState = intentState,
        sessionService = sessionService
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletRootView(
    buildContext: BuildContext,
    promptModel: PromptModel,
    intentState: IntentState,
    sessionService: SessionService
) {
    // PromptDialogs must be in the composition during NFC scanning so that
    // NfcTagReader.scan() can bind the PromptModel to UI. We remove it in two cases:
    //
    // 1. verifierNfcTransferActive=true (data transfer in progress): cancels the
    //    ScanNfcTagPromptDialog NoDialogState 3-second disableReaderMode() countdown,
    //    keeping the active isoDep connection alive.
    //
    // 2. verifierNfcTagDispatchSuppressed=REDISPATCH (post-transfer suppression):
    //    after the transfer, verifierNfcTransferActive goes false but REDISPATCH is set
    //    to prevent the still-present tag from being re-dispatched. Re-adding PromptDialogs
    //    at that point restarts the 3-second countdown, which then disables reader mode and
    //    ends the suppression — letting the system see the tag and showing "new tag detected".
    //    Keeping PromptDialogs removed while REDISPATCH is active avoids this.
    val verifierNfcActive by NfcTransferState.verifierNfcTransferActive.collectAsState()
    val verifierNfcSuppressed by NfcTransferState.verifierNfcTagDispatchSuppressed.collectAsState()
    if (!verifierNfcActive && verifierNfcSuppressed == NfcDispatchSuppressionMode.NONE) {
        PromptDialogs(promptModel)
    }

    App(
        sessionService = sessionService,
        intentState = intentState
    )
}

@ExperimentalMaterial3Api
@Composable
fun TransientFlowView(
    buildContext: BuildContext,
    promptModel: PromptModel,
    intentState: IntentState,
    sessionService: SessionService
) {
    TransientFlowRootView(
        buildContext = buildContext,
        promptModel = promptModel,
        intentState = intentState,
        sessionService = sessionService
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransientFlowRootView(
    buildContext: BuildContext,
    promptModel: PromptModel,
    intentState: IntentState,
    sessionService: SessionService
) {
    PromptDialogs(promptModel)

    TransientFlowApp(
        sessionService = sessionService,
        intentState = intentState
    )
}

public class AndroidPlatformAdapter(
    context: Context,
    private val intentState: IntentState
) : PlatformAdapter {
    private val applicationContext = context.applicationContext

    override fun getCameraPermission(): Boolean? {
        val permission = Manifest.permission.CAMERA
        return ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun openUrl(url: String) {
        Napier.d("Open URL: ${url.toUri()}")
        val uri = url.toUri()
        val customTabsIntent = CustomTabsIntent.Builder().build().apply {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            customTabsIntent.launchUrl(applicationContext, uri)
        } catch (e: Throwable) {
            Napier.w("Custom tab failed, falling back to browser intent", e)
            applicationContext.startActivity(
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    override fun tryOpenCrossDeviceQrCode(uri: String): Boolean =
        try {
            applicationContext.startActivity(
                Intent(Intent.ACTION_VIEW, uri.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        } catch (e: ActivityNotFoundException) {
            Napier.i("No system handler is available for cross-device QR codes", e)
            false
        } catch (e: SecurityException) {
            Napier.w("System handler rejected the cross-device QR code intent", e)
            false
        }

    override fun writeToFile(text: String, fileName: String, folderName: String) {
        val folder = File(applicationContext.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdir()
        }
        val file = File(folder, fileName)
        if (file.exists()) {
            file.appendText(text)
        } else {
            file.createNewFile()
            file.writeText(text)
        }
    }

    override fun readFromFile(fileName: String, folderName: String): String? {
        val folder = File(applicationContext.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdir()
        }
        return File(folder, fileName).takeIf { it.exists() }?.readText()
    }

    override fun clearFile(fileName: String, folderName: String) {
        val folder = File(applicationContext.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdir()
        }
        File(folder, fileName).takeIf { it.exists() }?.delete()
    }

    override fun shareLog() {
        val folder = File(applicationContext.filesDir, "logs")
        val file = File(folder, "log.txt")
        val fileUri = FileProvider.getUriForFile(
            applicationContext,
            "${applicationContext.packageName}.fileprovider",
            file,
        )

        val intent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "application/text"
        }
        applicationContext.startActivity(
            Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    override suspend fun registerWithDigitalCredentialsAPI(entries: CredentialRegistry, scope: CoroutineScope) {
        withContext(Dispatchers.Default) {
            catching {
                val credentialsListCbor = coseCompliantSerializer.encodeToByteArray(entries)
                val customRegistry = CustomRegistry(credentialsListCbor, applicationContext)
                val registryManager = RegistryManager.create(applicationContext)
                registryManager.clearCredentialRegistry(
                    ClearCredentialRegistryRequest(
                        ClearCredentialRegistryRequest.PerTypeConfig(
                            isDeleteAll = false,
                            type = DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
                            registryIds = listOf(applicationContext.packageName),
                        )
                    )
                )
                registryManager.registerCredentials(customRegistry)
                CustomRegistry.registerIssuance(applicationContext)
            }.onSuccess { Napier.i("DC API: Credential Manager registration succeeded") }
                .onFailure { Napier.w("DC API: Credential Manager registration failed", it) }
        }
    }

    // Source: https://github.com/openwallet-foundation/multipaz/blob/5c1845c400875edcc4620e395773d89c3f796256/multipaz-compose/src/androidMain/kotlin/org/multipaz/compose/digitalcredentials/CredentialManagerPresentmentActivity.kt#L206
    private data class SelectionInfo(
        val protocol: String,
        val documentIds: List<String>
    )

    private fun getSetSelection(request: ProviderGetCredentialRequest): SelectionInfo? {
        // TODO: replace sourceBundle peeking when we upgrade to a new Credman Jetpack..
        val setId = request.sourceBundle!!.getString("androidx.credentials.registry.provider.extra.CREDENTIAL_SET_ID")
            ?: return null
        val setElementLength = request.sourceBundle!!.getInt(
            "androidx.credentials.registry.provider.extra.CREDENTIAL_SET_ELEMENT_LENGTH", 0
        )
        val credIds = mutableListOf<String>()
        for (n in 0 until setElementLength) {
            val credId = request.sourceBundle!!.getString(
                "androidx.credentials.registry.provider.extra.CREDENTIAL_SET_ELEMENT_ID_$n"
            ) ?: return null
            val splits = credId.split(" ")
            require(splits.size == 3) { "Expected CredId $n to have three parts, got ${splits.size}" }
            credIds.add(splits[2])
        }
        val splits = setId.split(" ")
        require(splits.size == 2) { "Expected SetId to have two parts, got ${splits.size}" }
        return SelectionInfo(
            protocol = splits[1],
            documentIds = credIds
        )
    }

    private fun getSelection(request: ProviderGetCredentialRequest): SelectionInfo {
        val selectedEntryId = request.selectedEntryId
            ?: throw IllegalStateException("selectedEntryId is null")
        val splits = selectedEntryId.split(" ")
        require(splits.size == 3) { "Expected CredId to have three parts, got ${splits.size}" }
        return SelectionInfo(
            protocol = splits[1],
            documentIds = listOf(splits[2])
        )
    }

    private data class CallingAppMetadata(
        val packageName: String,
        val origin: String
    )

    private fun loadPrivilegedUserAgents(): String =
        applicationContext.assets.open("privileged_apps.json").use { stream ->
            val data = ByteArray(stream.available()).apply { stream.read(this) }
            data.decodeToString()
        }

    private fun resolveCallingAppMetadata(callingAppInfo: CallingAppInfo): CallingAppMetadata {
        val privilegedUserAgents = loadPrivilegedUserAgents()
        val callingOrigin = callingAppInfo.getOrigin(privilegedUserAgents)
            ?: callingAppInfo.toAndroidAppOrigin()
        return CallingAppMetadata(
            packageName = callingAppInfo.packageName,
            origin = callingOrigin
        )
    }

    private fun CallingAppInfo.toAndroidAppOrigin(): String {
        val currentSignatures = if (signingInfoCompat.hasMultipleSigners) {
            signingInfoCompat.apkContentsSigners
        } else {
            signingInfoCompat.signingCertificateHistory.take(1)
        }
        require(currentSignatures.size == 1) {
            "DC API: Calling app must have exactly one current signing certificate"
        }
        val certificateHash = MessageDigest.getInstance("SHA-256")
            .digest(currentSignatures.single().toByteArray())
        val encodedHash = Base64.encodeToString(
            certificateHash,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
        return "${OpenId4VpHolder.ANDROID_APK_KEY_HASH_ORIGIN_SCHEME}:$encodedHash"
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    override fun getCurrentDCAPIVerificationData(): KmmResult<RequestParametersFrom.DcApiRequest> = catching {
        (intentState.dcapiInvocationData.value as AndroidDCAPIInvocationData?)?.let { (intent, _) ->
            // Adapted from https://github.com/openwallet-foundation-labs/identity-credential/blob/d7a37a5c672ed6fe1d863cbaeb1a998314d19fc5/wallet/src/main/java/com/android/identity_credential/wallet/credman/CredmanPresentationActivity.kt#L74
            val credentialRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
                ?: throw IllegalArgumentException("DC API: No credential request received")

            val (callingPackageName, callingOrigin) =
                resolveCallingAppMetadata(credentialRequest.callingAppInfo)
            val option = credentialRequest.credentialOptions[0] as? GetDigitalCredentialOption
                ?: throw IllegalArgumentException("Expected GetDigitalCredentialOption object not received")

            Napier.d("DC API: Got request ${option.requestJson}")
            // Android's Bundle-to-JSON conversion currently serializes arrays as numerically-indexed objects
            // (e.g. ["a","b"] becomes {"0":"a","1":"b"}), which breaks deserialization of list properties.
            val dcRequestOptions = joseCompliantSerializer.decodeFromJsonElement(
                DigitalCredentialRequestOptions.serializer(),
                joseCompliantSerializer.parseToJsonElement(option.requestJson).coerceIndexedObjectsToArrays(),
            )

            Napier.d("DC API: Selection $dcRequestOptions")

            val selectionInfo = getSetSelection(credentialRequest)
                ?: getSelection(credentialRequest)

            Napier.d("DC API: Selection $selectionInfo")

            val credentialIds = selectionInfo.documentIds
            dcRequestOptions.toRequestParametersFrom(
                selectedProtocol = selectionInfo.protocol,
                credentialIds = credentialIds,
                callingPackageName = callingPackageName,
                callingOrigin = callingOrigin,
            )
        } ?: throw IllegalStateException("DCAPIInvocationData not set")
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    override fun getCurrentDCAPIIssuingData(): KmmResult<DCAPIIssuingRequest> = catching {
        (intentState.dcapiInvocationData.value as AndroidDCAPIInvocationData?)?.let { (intent, _) ->
            val credentialRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
                ?: throw IllegalArgumentException("DC API: No credential create request received")
            val (callingPackageName, callingOrigin) =
                resolveCallingAppMetadata(credentialRequest.callingAppInfo)
            val callingRequest = credentialRequest.callingRequest as? CreateDigitalCredentialRequest
                ?: throw IllegalArgumentException("Expected CreateDigitalCredentialRequest object not received")

            DCAPIIssuingRequest(
                requestJson = callingRequest.requestJson,
                callingPackageName = callingPackageName,
                callingOrigin = callingOrigin
            )
        } ?: throw IllegalStateException("DCAPIInvocationData not set")
    }

    override fun prepareDCAPICredentialResponse(response: DigitalCredentialInterface) {
        (intentState.dcapiInvocationData.value as AndroidDCAPIInvocationData?)?.let { (_, sendCredentialResponseToInvoker) ->
            val serializedResponse = response.toAndroidDcApiResponseJson()
            Napier.d("Returning response $serializedResponse")
            try {
                sendCredentialResponseToInvoker(serializedResponse, true)
            } finally {
                intentState.dcapiInvocationData.value = null
            }
        } ?: throw IllegalStateException("Callback for response not found")
    }

    override fun prepareDCAPICredentialError(error: String) {
        (intentState.dcapiInvocationData.value as AndroidDCAPIInvocationData?)?.let { (_, sendCredentialResponseToInvoker) ->
            try {
                sendCredentialResponseToInvoker(error, false)
            } finally {
                intentState.dcapiInvocationData.value = null
            }
        } ?: throw IllegalStateException("Callback for response not found")
    }

    override fun prepareDCAPIIssuingResponse(response: String, success: Boolean) {
        (intentState.dcapiInvocationData.value as AndroidDCAPIInvocationData?)?.let { (intent, sendCredentialResponseToInvoker) ->
            if (intent.action != RegistryManager.ACTION_CREATE_CREDENTIAL) {
                throw IllegalStateException("Expected DC API create invocation")
            }
            sendCredentialResponseToInvoker(response, success)
            intentState.dcapiInvocationData.value = null
        } ?: throw IllegalStateException("Callback for response not found")
    }

    override fun hasPendingDCAPIIssuingRequest(): Boolean =
        (intentState.dcapiInvocationData.value as AndroidDCAPIInvocationData?)
            ?.intent
            ?.action == RegistryManager.ACTION_CREATE_CREDENTIAL

    override fun openDeviceSettings() {
        Napier.d("Open Device settings")
        applicationContext.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", applicationContext.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Recursively undoes Android's Bundle-to-JSON conversion, which serializes arrays as objects.
     *
     * Two cases are handled:
     *  - Non-empty arrays become numerically-indexed objects (e.g. ["a","b"] -> {"0":"a","1":"b"}).
     *    Any object whose keys are exactly the contiguous integers 0..n-1 is converted back.
     *  - Empty arrays become empty objects ({} ). These are indistinguishable from genuine empty
     *    objects by shape alone, so they are only coerced for keys known to be arrays in the
     *    OpenID4VP / DCQL / DC API standards (see [openIdArrayKeys]).
     */
    private fun JsonElement.coerceIndexedObjectsToArrays(): JsonElement = when (this) {
        is JsonObject -> {
            val fixedChildren = mapValues { (key, value) ->
                val fixedValue = value.coerceIndexedObjectsToArrays()
                // A standardized array field still left as an object can only be a Bundle-encoded
                // (typically empty) array, so coerce it by index order.
                if (key in openIdArrayKeys && fixedValue is JsonObject) {
                    JsonArray(
                        fixedValue.entries
                            .sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
                            .map { it.value }
                    )
                } else {
                    fixedValue
                }
            }
            val indices = fixedChildren.keys.mapNotNull { it.toIntOrNull() }
            if (indices.size == fixedChildren.size && indices.isNotEmpty() &&
                indices.sorted() == (0 until fixedChildren.size).toList()
            ) {
                JsonArray(fixedChildren.entries.sortedBy { it.key.toInt() }.map { it.value })
            } else {
                JsonObject(fixedChildren)
            }
        }

        is JsonArray -> JsonArray(map { it.coerceIndexedObjectsToArrays() })
        else -> this
    }

    private companion object {
        /**
         * Serialized names of array-valued properties across the OpenID4VP request, its `dcql_query`,
         * `client_metadata`, `transaction_data` and ISO 18013-7 DC API structures. Used to recover
         * empty arrays that Android's Bundle-to-JSON conversion collapsed into empty objects.
         */
        private val openIdArrayKeys = setOf(
            // OpenID4VP / DC API request
            "requests",
            "redirect_uris",
            "expected_origins",
            "verifier_info",
            "transaction_data",
            "transaction_data_hashes",
            // DCQL query
            "credentials",
            "credential_sets",
            "claims",
            "claim_sets",
            "trusted_authorities",
            "values",
            "vct_values",
            "type_values",
            "options",
            "path",
            // ISO 18013-7 / mdoc document request structures
            "documentDigests",
            "documentLocations",
            "documentSets",
            "alternativeDataElements",
            "alternativeElementSets",
            "recipientCertificate",
            "status_lists",
            "systemSpecs",
            "useCases",
        )
    }

}

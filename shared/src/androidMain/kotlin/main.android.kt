import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import at.asitplus.wallet.app.common.presentation.NfcDispatchSuppressionMode
import at.asitplus.wallet.app.common.presentation.NfcTransferState
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.credentials.CreateDigitalCredentialRequest
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.registry.provider.RegistryManager
import androidx.credentials.registry.provider.selectedEntryId
import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.dcapi.*
import at.asitplus.dcapi.request.ExchangeProtocolIdentifier
import at.asitplus.dcapi.request.verifier.DigitalCredentialGetRequest
import at.asitplus.dcapi.request.verifier.DigitalCredentialRequestOptions
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.signum.indispensable.josef.typed
import at.asitplus.wallet.app.android.dcapi.AndroidDCAPIInvocationData
import at.asitplus.wallet.app.android.dcapi.CustomRegistry
import at.asitplus.wallet.app.common.*
import at.asitplus.wallet.app.common.dcapi.DCAPIIssuingRequest
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.prompt.PromptModel
import ui.theme.darkScheme
import ui.theme.lightScheme
import java.io.File
import kotlin.io.encoding.ExperimentalEncodingApi


actual fun getPlatformName(): String = "Android"


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
    private val context: Context,
    private val intentState: IntentState
) : PlatformAdapter {

    override fun getCameraPermission(): Boolean? {
        (context as? Activity)?.let { activity ->
            val permission = Manifest.permission.CAMERA
            return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
        return null
    }

    override fun openUrl(url: String) {
        Napier.d("Open URL: ${url.toUri()}")
        val uri = url.toUri()
        val customTabsIntent = CustomTabsIntent.Builder().build().apply {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        try {
            customTabsIntent.launchUrl(context, uri)
        } catch (e: Throwable) {
            Napier.w("Custom tab failed, falling back to browser intent", e)
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
            )
        }
    }

    override fun writeToFile(text: String, fileName: String, folderName: String) {
        val folder = File(context.filesDir, folderName)
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
        val folder = File(context.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdir()
        }
        return File(folder, fileName).takeIf { it.exists() }?.readText()
    }

    override fun clearFile(fileName: String, folderName: String) {
        val folder = File(context.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdir()
        }
        File(folder, fileName).takeIf { it.exists() }?.delete()
    }

    override fun shareLog() {
        val folder = File(context.filesDir, "logs")
        val file = File(folder, "log.txt")
        val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "application/text"
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    override fun registerWithDigitalCredentialsAPI(entries: CredentialRegistry, scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            catching {
                val credentialsListCbor = coseCompliantSerializer.encodeToByteArray(entries)
                val customRegistry = CustomRegistry(credentialsListCbor, context)
                RegistryManager.create(context).registerCredentials(customRegistry)
                CustomRegistry.registerIssuance(context)
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
        context.assets.open("privileged_apps.json").use { stream ->
            val data = ByteArray(stream.available()).apply { stream.read(this) }
            data.decodeToString()
        }

    private fun resolveCallingAppMetadata(callingAppInfo: CallingAppInfo): CallingAppMetadata {
        val privilegedUserAgents = loadPrivilegedUserAgents()
        val callingOrigin = callingAppInfo.getOrigin(privilegedUserAgents)
            ?: throw IllegalArgumentException("DC API: Calling app origin unknown")
        return CallingAppMetadata(
            packageName = callingAppInfo.packageName,
            origin = callingOrigin
        )
    }

    @OptIn(ExperimentalDigitalCredentialApi::class, ExperimentalEncodingApi::class)
    override fun getCurrentDCAPIVerificationData(): KmmResult<RequestParametersFrom.DcApiRequest> = catching {
        (intentState.dcapiInvocationData.value as AndroidDCAPIInvocationData?)?.let { (intent, _) ->
            // Adapted from https://github.com/openwallet-foundation-labs/identity-credential/blob/d7a37a5c672ed6fe1d863cbaeb1a998314d19fc5/wallet/src/main/java/com/android/identity_credential/wallet/credman/CredmanPresentationActivity.kt#L74
            val credentialRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
                ?: throw IllegalArgumentException("DC API: No credential request received")

            val (callingPackageName, callingOrigin) =
                resolveCallingAppMetadata(credentialRequest.callingAppInfo)
            val option = credentialRequest.credentialOptions[0] as? GetDigitalCredentialOption
                ?: throw IllegalArgumentException("Expected GetDigitalCredentialOption object not received")

            // Android's Bundle-to-JSON conversion serializes empty (and numerically-indexed) arrays as
            // objects, which breaks deserialization of `redirect_uris` in client_metadata.
            val rawRequest = joseCompliantSerializer.parseToJsonElement(option.requestJson)
                .fixRedirectUrisArrayShape()
            val dcRequestOptions = joseCompliantSerializer.decodeFromJsonElement(
                DigitalCredentialRequestOptions.serializer(),
                rawRequest,
            )

            val selectionInfo = getSetSelection(credentialRequest)
                ?: getSelection(credentialRequest)

            val digitalCredentialGetRequest =
                dcRequestOptions.requests.find { it.protocol == ExchangeProtocolIdentifier(selectionInfo.protocol) }
                    ?: throw IllegalStateException("Unable to find suitable DC API request. Protocol may not be supported.")

            Napier.d("DC API: Got request ${option.requestJson} for selection $selectionInfo")

            val credentialIds = selectionInfo.documentIds

            when (digitalCredentialGetRequest) {
                is DigitalCredentialGetRequest.OpenId4VpSigned -> {
                    Napier.d("Using OpenID4VP Signed, got request $digitalCredentialGetRequest for credential IDs $credentialIds")
                    RequestParametersFrom.OpenId4VpDcApiSigned(
                        jwsTyped = digitalCredentialGetRequest.data.request.typed(),
                        credentialIds = credentialIds,
                        callingPackageName = callingPackageName,
                        callingOrigin = callingOrigin
                    )
                }
                is DigitalCredentialGetRequest.OpenId4VpMultiSigned -> {
                    TODO("OpenID4VP multisigned DC API requests are not supported yet")
                }
                is DigitalCredentialGetRequest.OpenId4VpUnsigned -> {
                    Napier.d("Using OpenID4VP Unsigned, got request $digitalCredentialGetRequest for credential IDs $credentialIds")
                    RequestParametersFrom.OpenId4VpDcApiUnsigned(
                        parameters = digitalCredentialGetRequest.data,
                        jsonString = joseCompliantSerializer.encodeToString(digitalCredentialGetRequest.data),
                        credentialIds = credentialIds,
                        callingPackageName = callingPackageName,
                        callingOrigin = callingOrigin
                    )
                }
                is DigitalCredentialGetRequest.IsoMdoc -> {
                    Napier.d("Using Iso 18013-7 Annex C, got request $digitalCredentialGetRequest for credential IDs $credentialIds")
                    RequestParametersFrom.IsoMdocDcApi(
                        parameters = RequestParametersFrom.IsoMdocDcApi.IsoMdocRequestWrapper(
                            digitalCredentialGetRequest.data
                        ),
                        jsonString = joseCompliantSerializer.encodeToString(digitalCredentialGetRequest.data),
                        credentialIds = credentialIds,
                        callingPackageName = callingPackageName,
                        callingOrigin = callingOrigin
                    )
                }
            }
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

    override fun prepareDCAPICredentialResponse(response: String, success: Boolean) {
        (intentState.dcapiInvocationData.value as AndroidDCAPIInvocationData?)?.let { (_, sendCredentialResponseToInvoker) ->
            sendCredentialResponseToInvoker(response, success)
            intentState.dcapiInvocationData.value = null
        } ?: throw IllegalStateException("Callback for response not found")
    }

    override fun prepareIsoMdocDCAPICredentialResponse(response: EncryptedResponse, success: Boolean) {
        (intentState.dcapiInvocationData.value as AndroidDCAPIInvocationData?)?.let { (_, sendCredentialResponseToInvoker) ->
            intentState.dcapiInvocationData.value = null
            Napier.d("Returning response $response to digital credentials API invoker")
            val dcApiResponse = DCAPIResponse(response)
            // Needs to be cast to DigitalCredentialInterface so that protocol member is serialized
            val isoMdocResponse: DigitalCredentialInterface = IsoMdocResponse(dcApiResponse)
            val serializedResponse = joseCompliantSerializer.encodeToString(isoMdocResponse)
            Napier.d("Returning response $serializedResponse")
            sendCredentialResponseToInvoker(serializedResponse, success)
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
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun JsonElement.fixRedirectUrisArrayShape(): JsonElement = when (this) {
        is JsonObject -> JsonObject(mapValues { (key, value) ->
            if (key == "redirect_uris" && value is JsonObject) {
                JsonArray(
                    value.entries
                        .sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
                        .map { it.value }
                )
            } else {
                value.fixRedirectUrisArrayShape()
            }
        })
        is JsonArray -> JsonArray(map { it.fixRedirectUrisArrayShape() })
        else -> this
    }

}

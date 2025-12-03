import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.registry.provider.RegistryManager
import androidx.credentials.registry.provider.selectedEntryId
import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.dcapi.DCAPIResponse
import at.asitplus.dcapi.EncryptedResponse
import at.asitplus.dcapi.EncryptedResponseData
import at.asitplus.dcapi.request.DCAPIRequest
import at.asitplus.dcapi.request.IsoMdocRequest
import at.asitplus.dcapi.request.Oid4vpDCAPIRequest
import at.asitplus.iso.DeviceRequest
import at.asitplus.iso.EncryptionInfo
import at.asitplus.iso.EncryptionParameters
import at.asitplus.signum.indispensable.cosef.CoseKeyParams.EcKeyParams
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.signum.indispensable.io.Base64UrlStrict
import at.asitplus.wallet.app.android.dcapi.CustomRegistry
import at.asitplus.wallet.app.android.dcapi.DCAPIInvocationData
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.CapabilitiesService
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.RealCapabilitiesService
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import at.asitplus.wallet.app.common.di.appModule
import at.asitplus.wallet.lib.data.vckJsonSerializer
import data.storage.RealDataStoreService
import data.storage.getDataStore
import io.github.aakira.napier.Napier
import io.matthewnelson.encoding.core.Decoder.Companion.decodeToByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.json.JSONObject
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate
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

@SuppressLint("ViewModelConstructorInComposable")
@Composable
fun MainView(
    buildContext: BuildContext,
    promptModel: PromptModel
) {
    val platformAdapter = AndroidPlatformAdapter(LocalContext.current)
    val dataStoreService = RealDataStoreService(
        getDataStore(LocalContext.current),
        platformAdapter
    )
    val ks = KeystoreService(dataStoreService)

    PromptDialogs(promptModel)

    val walletDependencyProvider = WalletDependencyProvider(
        keystoreService = ks,
        dataStoreService = dataStoreService,
        platformAdapter = platformAdapter,
        buildContext = buildContext,
        promptModel = promptModel
    )

    val capabilitiesModule = module {
        scope(named(SESSION_NAME)) {
            scopedOf(::RealCapabilitiesService) binds arrayOf(CapabilitiesService::class)
        }
    }
    val module = appModule(walletDependencyProvider, capabilitiesModule)

    App(module)
}

public class AndroidPlatformAdapter(
    private val context: Context
) : PlatformAdapter {

    override fun getCameraPermission(): Boolean? {
        (context as? Activity)?.let { activity ->
            val permission = Manifest.permission.CAMERA
            return when {
                ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED -> {
                    true
                }
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) -> {
                    false
                }
                else -> {
                    null
                }
            }
        }
        return null
    }

    override fun openUrl(url: String) {
        Napier.d("Open URL: ${url.toUri()}")
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
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

    private fun getSelection(request: ProviderGetCredentialRequest): SelectionInfo? {
        val selectedEntryId = request.selectedEntryId
            ?: throw IllegalStateException("selectedEntryId is null")
        val splits = selectedEntryId.split(" ")
        require(splits.size == 3) { "Expected CredId to have three parts, got ${splits.size}" }
        return SelectionInfo(
            protocol = splits[1],
            documentIds = listOf(splits[2])
        )
    }

    @OptIn(ExperimentalDigitalCredentialApi::class, ExperimentalEncodingApi::class)
    override fun getCurrentDCAPIData(): KmmResult<DCAPIRequest> = catching {
        (Globals.dcapiInvocationData.value as DCAPIInvocationData?)?.let { (intent, _) ->
            // Adapted from https://github.com/openwallet-foundation-labs/identity-credential/blob/d7a37a5c672ed6fe1d863cbaeb1a998314d19fc5/wallet/src/main/java/com/android/identity_credential/wallet/credman/CredmanPresentationActivity.kt#L74
            val credentialRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
                ?: throw IllegalArgumentException("DC API: No credential request received")

            val privilegedUserAgents =
                context.assets.open("privileged_apps.json").use { stream ->
                    val data = ByteArray(stream.available()).apply { stream.read(this) }
                    data.decodeToString()
                }

            val callingAppInfo = credentialRequest.callingAppInfo
            val callingPackageName = callingAppInfo.packageName
            val callingOrigin = callingAppInfo.getOrigin(privilegedUserAgents)
            //?: getAppOrigin(callingAppInfo.signingInfoCompat.signingCertificateHistory[0].toByteArray())
                ?: throw IllegalArgumentException("DC API: Calling app origin unknown")
            val option = credentialRequest.credentialOptions[0] as GetDigitalCredentialOption
            val requestJson = JSONObject(option.requestJson)

            val selectionInfo = getSetSelection(credentialRequest)
                ?: getSelection(credentialRequest)
                ?: throw IllegalStateException("Unable to get DC API selection")

            Napier.d("DC API: Got request $requestJson for selection $selectionInfo")


            val parsedRequest = if (requestJson.has("providers")) {
                requestJson.getJSONArray("providers").getJSONObject(0)
            } else {
                requestJson.getJSONArray("requests").getJSONObject(0)
            } // Only first request supported for now

            val protocol = parsedRequest.getString("protocol")
            val requestData = if (parsedRequest.has("request")) {
                JSONObject(parsedRequest.getString("request"))
            } else {
                parsedRequest.getJSONObject("data")
            }

            val credentialId = selectionInfo.documentIds[0] // selectionInfo.documentIds
            // TODO support multiple documents, need vck composite build for it

            when {
                protocol.startsWith("openid4vp") -> {
                    Napier.d("Using protocol $protocol, got request $requestData for credential ID $credentialId")
                    Oid4vpDCAPIRequest(
                        protocol, requestData.toString(), credentialId, callingPackageName, callingOrigin
                    )
                }

                protocol == "org.iso.mdoc" || protocol == "org-iso-mdoc" -> {
                    val deviceRequest = requestData.getString("deviceRequest")
                    val encryptionInfo = requestData.getString("encryptionInfo")
                    val parsedDeviceRequest = coseCompliantSerializer.decodeFromByteArray<DeviceRequest>(
                        deviceRequest.decodeToByteArray(Base64UrlStrict)
                    )
                    val parsedEncryptionInfo = coseCompliantSerializer.decodeFromByteArray<EncryptionInfo>(
                        encryptionInfo.decodeToByteArray(Base64UrlStrict)
                    )
                    IsoMdocRequest(
                        parsedDeviceRequest,
                        parsedEncryptionInfo,
                        credentialId,
                        callingPackageName,
                        callingOrigin
                    )
                }

                else -> {
                    Napier.e("DC API: Protocol type $protocol not supported")
                    throw IllegalArgumentException("Protocol type $protocol not supported")
                }
            }
        } ?: throw IllegalStateException("DCAPIInvocationData not set")
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun prepareDCAPIIsoMdocCredentialResponse(
        responseJson: ByteArray,
        sessionTranscript: ByteArray,
        encryptionParameters: EncryptionParameters
    ) {
        (Globals.dcapiInvocationData.value as DCAPIInvocationData?)?.let { (_, sendCredentialResponseToInvoker) ->
            val publicKey = try {
                val x = (encryptionParameters.recipientPublicKey.keyParams as EcKeyParams<*>).x
                val y = (encryptionParameters.recipientPublicKey.keyParams as EcKeyParams<*>).y
                EcPublicKeyDoubleCoordinate(EcCurve.P256, x!!, y!! as ByteArray)
            } catch (e: Throwable) {
                Napier.e("Could not extract public key", e)
                throw IllegalArgumentException("Could not extract public key")
            }

            val (cipherText, encapsulatedPublicKey) = Crypto.hpkeEncrypt(
                Algorithm.HPKE_BASE_P256_SHA256_AES128GCM,
                publicKey,
                responseJson,
                sessionTranscript
            )

            encapsulatedPublicKey as EcPublicKeyDoubleCoordinate
            val encryptedResponseData = EncryptedResponseData(
                enc = encapsulatedPublicKey.asUncompressedPointEncoding,
                cipherText = cipherText
            )
            val encryptedResponse = EncryptedResponse("dcapi", encryptedResponseData)

            val dcApiResponse = DCAPIResponse.createIsoMdocResponse(encryptedResponse)
            Napier.d("Returning response $responseJson to digital credentials API invoker")
            sendCredentialResponseToInvoker(vckJsonSerializer.encodeToString(dcApiResponse), true)
        } ?: throw IllegalStateException("Callback for response not found")
    }

    override fun prepareDCAPIOid4vpCredentialResponse(responseJson: String, success: Boolean) {
        (Globals.dcapiInvocationData.value as DCAPIInvocationData?)?.let { (_, sendCredentialResponseToInvoker) ->
            Napier.d("Returning response $responseJson to digital credentials API invoker")
            sendCredentialResponseToInvoker(responseJson, success)
        } ?: throw IllegalStateException("Callback for response not found")
    }

    override fun openDeviceSettings() {
        Napier.d("Open Device settings")
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
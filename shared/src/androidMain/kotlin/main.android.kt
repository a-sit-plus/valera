import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.PendingIntentHandler
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
import at.asitplus.openid.OpenIdConstants.DC_API_OID4VP_PROTOCOL_IDENTIFIER
import at.asitplus.signum.indispensable.cosef.CoseKeyParams.EcKeyParams
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.signum.indispensable.io.Base64UrlStrict
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.android.dcapi.DCAPIAndroidExporter
import at.asitplus.wallet.app.android.dcapi.DCAPIInvocationData
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.WalletKeyMaterial
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialList
import at.asitplus.wallet.app.common.dcapi.data.preview.ResponseJSON
import at.asitplus.wallet.lib.data.vckJsonSerializer
import com.android.identity.android.mdoc.util.CredmanUtil
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import data.storage.RealDataStoreService
import data.storage.getDataStore
import io.github.aakira.napier.Napier
import io.matthewnelson.encoding.core.Decoder.Companion.decodeToByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.json.JSONObject
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate
import org.multipaz.prompt.AndroidPromptModel
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

@Composable
fun MainView(
    buildContext: BuildContext,
) {
    val promptModel = AndroidPromptModel()
    val platformAdapter = AndroidPlatformAdapter(LocalContext.current)
    val dataStoreService = RealDataStoreService(
        getDataStore(LocalContext.current),
        platformAdapter
    )
    val ks = KeystoreService(dataStoreService)

    PromptDialogs(promptModel)

    App(
        WalletDependencyProvider(
            keystoreService = ks,
            dataStoreService = dataStoreService,
            platformAdapter = platformAdapter,
            buildContext = buildContext,
            promptModel = promptModel
        )
    )
}

public class AndroidPlatformAdapter(
    private val context: Context
) : PlatformAdapter {

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
        val file = File(folder, fileName)
        return if (file.exists()) {
            file.readText()
        } else {
            null
        }
    }

    override fun clearFile(fileName: String, folderName: String) {
        val folder = File(context.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdir()
        }
        val file = File(folder, fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    override fun shareLog() {
        val folder = File(context.filesDir, "logs")
        val file = File(folder, "log.txt")
        val fileUri = FileProvider.getUriForFile(
            context,
            "at.asitplus.wallet.app.android.fileprovider",
            file
        )


        val intent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "application/text"
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    override fun registerWithDigitalCredentialsAPI(entries: CredentialList, scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            catching {
                val client = IdentityCredentialManager.Companion.getClient(context)
                val credentialsListCbor = coseCompliantSerializer.encodeToByteArray(entries)
                val exporter = DCAPIAndroidExporter(context)
                val registrationRequest = exporter.createRegistrationRequest(credentialsListCbor)

                client.registerCredentials(registrationRequest).await()
            }.onSuccess { Napier.i("DC API: Credential Manager registration succeeded") }
                .onFailure { Napier.w("DC API: Credential Manager registration failed", it) }
        }
    }

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalDigitalCredentialApi::class, ExperimentalEncodingApi::class)
    override fun getCurrentDCAPIData(): KmmResult<DCAPIRequest> = catching {
        (Globals.dcapiInvocationData.value as DCAPIInvocationData?)?.let { (intent, _) ->
            // Adapted from https://github.com/openwallet-foundation-labs/identity-credential/blob/d7a37a5c672ed6fe1d863cbaeb1a998314d19fc5/wallet/src/main/java/com/android/identity_credential/wallet/credman/CredmanPresentationActivity.kt#L74
            val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
            val credentialId = request!!.selectedEntryId!!

            val privilegedUserAgents =
                context.assets.open("privileged_apps.json").use { stream ->
                    val data = ByteArray(stream.available()).apply { stream.read(this) }
                    data.decodeToString()
                }

            val callingAppInfo = request.callingAppInfo
            val callingPackageName = callingAppInfo.packageName
            val callingOrigin = callingAppInfo.getOrigin(privilegedUserAgents)
                ?: throw IllegalArgumentException("Origin unknown")
            val option = request.credentialOptions[0] as GetDigitalCredentialOption
            val requestJson = JSONObject(option.requestJson)

            //val cmrequest = IntentHelper.extractGetCredentialRequest(it) ?: return null
            //val credentialId = it.getStringExtra(IntentHelper.EXTRA_CREDENTIAL_ID)?.toInt() ?: -1

            // This call is currently broken, have to extract this info manually for now
            //val callingAppInfo = extractCallingAppInfo(intent)
            //val callingPackageName = it.getStringExtra("androidx.identitycredentials.extra.CALLING_PACKAGE_NAME") // IntentHelper.EXTRA_CALLING_PACKAGE_NAME produces InterpreterMethodNotFoundError
            //val callingOrigin = it.getStringExtra("androidx.identitycredentials.extra.ORIGIN") // IntentHelper.EXTRA_ORIGIN produces InterpreterMethodNotFoundError

            //val json = JSONObject(cmrequest.credentialOptions[0].requestMatcher)

            Napier.d("Got request $requestJson for credential ID $credentialId")

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

            when {
                protocol == "preview" -> {
                    // Extract params from the preview protocol request
                    val requestedData = mutableMapOf<String, MutableList<Pair<String, Boolean>>>()
                    //val previewRequest = JSONObject(parsedRequest)
                    val selector = requestData.getJSONObject("selector")
                    val nonceBase64 = requestData.getString("nonce")
                    val readerPublicKeyBase64 = requestData.getString("readerPublicKey")
                    val docType = selector.getString("doctype")

                    // Convert nonce and publicKey
                    val nonce = Base64.decode(nonceBase64, Base64.NO_WRAP or Base64.URL_SAFE)

                    // Match all the requested fields
                    val fields = selector.getJSONArray("fields")
                    for (n in 0 until fields.length()) {
                        val field = fields.getJSONObject(n)
                        val name = field.getString("name")
                        val namespace = field.getString("namespace")
                        val intentToRetain = field.getBoolean("intentToRetain")
                        requestedData.getOrPut(namespace) { mutableListOf() }
                            .add(Pair(name, intentToRetain))
                    }

                    at.asitplus.dcapi.request.PreviewDCAPIRequest(
                        requestData.toString(),
                        requestedData,
                        credentialId,
                        callingPackageName,
                        callingOrigin,
                        nonce,
                        readerPublicKeyBase64,
                        docType,
                    )
                }
                protocol.startsWith(DC_API_OID4VP_PROTOCOL_IDENTIFIER) -> {
                    Napier.d("Using protocol $protocol, got request $requestData for credential ID $credentialId")
                    Oid4vpDCAPIRequest(
                        protocol, requestData.toString(), credentialId, callingPackageName, callingOrigin
                    )
                }

                protocol == "org.iso.mdoc" || protocol == "org-iso-mdoc"  -> {
                    val deviceRequest = requestData.getString("deviceRequest")
                    val encryptionInfo = requestData.getString("encryptionInfo")
                    val parsedDeviceRequest =
                        coseCompliantSerializer.decodeFromByteArray<DeviceRequest>(
                            deviceRequest.decodeToByteArray(Base64UrlStrict)
                        )
                    val parsedEncryptionInfo =
                        coseCompliantSerializer.decodeFromByteArray<EncryptionInfo>(
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
                    Napier.e("Protocol type $protocol not supported")
                    throw IllegalArgumentException("Protocol type $protocol not supported")
                }
            }
        } ?: throw IllegalStateException("DCAPIInvocationData not set")
    }

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalEncodingApi::class)
    override fun prepareDCAPIPreviewCredentialResponse(
        responseJson: ByteArray,
        dcApiRequestPreview: at.asitplus.dcapi.request.PreviewDCAPIRequest
    ) {
        val readerPublicKey = EcPublicKeyDoubleCoordinate.fromUncompressedPointEncoding(
            EcCurve.P256,
            Base64.decode(
                dcApiRequestPreview.readerPublicKeyBase64,
                Base64.NO_WRAP or Base64.URL_SAFE
            )
        )
        // Generate the Session Transcript
        val encodedSessionTranscript = if (dcApiRequestPreview.callingOrigin == null) {
            CredmanUtil.generateAndroidSessionTranscript(
                dcApiRequestPreview.nonce,
                dcApiRequestPreview.callingPackageName!!,
                Crypto.digest(Algorithm.SHA256, readerPublicKey.asUncompressedPointEncoding)
            )
        } else {
            CredmanUtil.generateBrowserSessionTranscript(
                dcApiRequestPreview.nonce,
                dcApiRequestPreview.callingOrigin!!,
                Crypto.digest(Algorithm.SHA256, readerPublicKey.asUncompressedPointEncoding)
            )
        }

        val (cipherText, encapsulatedPublicKey) = Crypto.hpkeEncrypt(
            Algorithm.HPKE_BASE_P256_SHA256_AES128GCM,
            readerPublicKey,
            responseJson,
            encodedSessionTranscript
        )
        val encodedCredentialDocument =
            CredmanUtil.generateCredentialDocument(cipherText, encapsulatedPublicKey)

        val response =
            ResponseJSON(kotlin.io.encoding.Base64.UrlSafe.encode(encodedCredentialDocument))
        (Globals.dcapiInvocationData.value as DCAPIInvocationData?)?.let { (_, sendCredentialResponseToInvoker) ->
            sendCredentialResponseToInvoker(joseCompliantSerializer.encodeToString(response), true)
        } ?: throw IllegalStateException("Callback for response not found")
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
}

actual fun getImageDecoder(image: ByteArray): ImageBitmap {
    val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
    return bitmap.asImageBitmap()
}

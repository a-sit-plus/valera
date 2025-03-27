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
import at.asitplus.wallet.app.android.AndroidCryptoService
import at.asitplus.wallet.app.android.dcapi.DCAPIInvocationData
import at.asitplus.wallet.app.android.dcapi.IdentityCredentialHelper
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.dcapi.CredentialsContainer
import at.asitplus.wallet.app.common.dcapi.DCAPIRequest
import at.asitplus.wallet.app.common.dcapi.ResponseJSON
import com.android.identity.android.mdoc.util.CredmanUtil
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import com.google.android.gms.identitycredentials.IntentHelper
import data.storage.RealDataStoreService
import data.storage.getDataStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate
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
    sendCredentialResponseToDCAPIInvokerMethod: (String) -> Unit
) {
    val scope = CoroutineScope(Dispatchers.Default)
    val platformAdapter = AndroidPlatformAdapter(LocalContext.current, sendCredentialResponseToDCAPIInvokerMethod, scope)
    val dataStoreService = RealDataStoreService(
        getDataStore(LocalContext.current),
        platformAdapter
    )
    val ks = KeystoreService(dataStoreService)

    App(
        WalletMain(
            cryptoService = ks.let { runBlocking { AndroidCryptoService(it.getSigner()) } },
            dataStoreService = dataStoreService,
            platformAdapter = platformAdapter,
            buildContext = buildContext,
            scope = scope
        )
    )
}

class AndroidPlatformAdapter(
    private val context: Context,
    private val sendCredentialResponseToDCAPIInvoker: (String) -> Unit,
    private val scope: CoroutineScope
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

    override fun registerWithDigitalCredentialsAPI(entries: CredentialsContainer) {
        val registry = IdentityCredentialHelper(entries, this)
        val client = IdentityCredentialManager.Companion.getClient(context)
        client.registerCredentials(registry.toRegistrationRequest(context))
            .addOnSuccessListener { Napier.i("DCAPI: Credential Manager registration succeeded") }
            .addOnFailureListener { Napier.w("DCAPI: Credential Manager registration failed", it) }
    }

    override fun getCurrentDCAPIData(): DCAPIRequest? {
        return (Globals.dcapiInvocationData.value as DCAPIInvocationData?)?.intent?.let {
            // Adapted from https://github.com/openwallet-foundation-labs/identity-credential/blob/d7a37a5c672ed6fe1d863cbaeb1a998314d19fc5/wallet/src/main/java/com/android/identity_credential/wallet/credman/CredmanPresentationActivity.kt#L74
            val cmrequest = IntentHelper.extractGetCredentialRequest(it) ?: return null
            val credentialId = it.getLongExtra(IntentHelper.EXTRA_CREDENTIAL_ID, -1).toInt()

            // This call is currently broken, have to extract this info manually for now
            //val callingAppInfo = extractCallingAppInfo(intent)
            val callingPackageName =
                it.getStringExtra("androidx.identitycredentials.extra.CALLING_PACKAGE_NAME") // IntentHelper.EXTRA_CALLING_PACKAGE_NAME produces InterpreterMethodNotFoundError
            val callingOrigin =
                it.getStringExtra("androidx.identitycredentials.extra.ORIGIN") // IntentHelper.EXTRA_ORIGIN produces InterpreterMethodNotFoundError

            if (callingPackageName == null && callingOrigin == null) {
                Napier.w("Neither calling package name nor origin known")
                return null
            }

            val requestedData = mutableMapOf<String, MutableList<Pair<String, Boolean>>>()

            val json = JSONObject(cmrequest.credentialOptions[0].requestMatcher)
            val provider = json.getJSONArray("providers").getJSONObject(0)

            val protocol = provider.getString("protocol")
            val request = provider.getString("request")

            if (protocol == "preview") {
                // Extract params from the preview protocol request
                val previewRequest = JSONObject(request)
                val selector = previewRequest.getJSONObject("selector")
                val nonceBase64 = previewRequest.getString("nonce")
                val readerPublicKeyBase64 = previewRequest.getString("readerPublicKey")
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
                    requestedData.getOrPut(namespace) { mutableListOf() }.add(Pair(name, intentToRetain))
                }

                DCAPIRequest(
                    request,
                    requestedData,
                    credentialId,
                    callingPackageName,
                    callingOrigin,
                    nonce,
                    readerPublicKeyBase64,
                    docType
                )
            } else {
                Napier.w("Protocol type not supported")
                null
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun prepareDCAPICredentialResponse(responseJson: ByteArray, dcApiRequest: DCAPIRequest) {
        val readerPublicKey = EcPublicKeyDoubleCoordinate.fromUncompressedPointEncoding(
            EcCurve.P256,
            Base64.decode(dcApiRequest.readerPublicKeyBase64, Base64.NO_WRAP or Base64.URL_SAFE)
        )
        // Generate the Session Transcript
        val encodedSessionTranscript = if (dcApiRequest.callingOrigin == null) {
            CredmanUtil.generateAndroidSessionTranscript(
                dcApiRequest.nonce,
                dcApiRequest.callingPackageName!!,
                Crypto.digest(Algorithm.SHA256, readerPublicKey.asUncompressedPointEncoding)
            )
        } else {
            CredmanUtil.generateBrowserSessionTranscript(
                dcApiRequest.nonce,
                dcApiRequest.callingOrigin,
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

        val response = ResponseJSON(kotlin.io.encoding.Base64.UrlSafe.encode(encodedCredentialDocument))
        sendCredentialResponseToDCAPIInvoker(response.serialize())
    }
}

actual fun getImageDecoder(image: ByteArray): ImageBitmap {
    val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
    return bitmap.asImageBitmap()
}
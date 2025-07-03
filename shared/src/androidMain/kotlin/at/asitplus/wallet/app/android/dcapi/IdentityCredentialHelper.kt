package at.asitplus.wallet.app.android.dcapi

import AndroidPlatformAdapter
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import at.asitplus.wallet.app.common.decodeImage
import com.google.android.gms.identitycredentials.RegistrationRequest
import at.asitplus.wallet.app.common.dcapi.data.preview.CredentialsContainer
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.collections.forEach
import kotlin.text.toByteArray
import androidx.core.graphics.scale

// adapted from https://github.com/openwallet-foundation-labs/identity-credential/blob/d7a37a5c672ed6fe1d863cbaeb1a998314d19fc5/appholder/src/main/java/com/android/identity/wallet/credman/IdentityCredentialRegistry.kt
class IdentityCredentialHelper(
    private val credentialsContainer: CredentialsContainer,
    private val androidPlatformAdapter: AndroidPlatformAdapter,
) {


    private fun credentialBytes(): ByteArray {
        val icons = ByteArrayOutputStream()
        val iconSizeList = mutableListOf<Int>()
        val entries = credentialsContainer.credentials
        entries.forEach { entry ->
            val iconBytes = getIconBytes(
                entry.icon,
                androidPlatformAdapter = androidPlatformAdapter
            ) //TODO what to do on empty image
            entry.credential.displayInfo.iconId = iconSizeList.size
            iconSizeList.add(iconBytes.size())
            iconBytes.writeTo(icons)
        }
        val credsBytes = Json{ explicitNulls = false }.encodeToString(credentialsContainer).toByteArray()
        val result = ByteArrayOutputStream()
        // header_size
        result.write(intBytes((3 + iconSizeList.size) * Int.SIZE_BYTES))
        // creds_size
        result.write(intBytes(credsBytes.size))
        // icon_size_array_size
        result.write(intBytes(iconSizeList.size))
        // icon offsets
        iconSizeList.forEach { result.write(intBytes(it)) }
        result.write(credsBytes)
        icons.writeTo(result)
        return result.toByteArray()
    }

    companion object {
        fun intBytes(num: Int): ByteArray =
            ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(num).array()

        fun toRegistrationRequest(context: Context, credentialsListCbor: ByteArray): RegistrationRequest {
            return RegistrationRequest(
                credentials = credentialsListCbor,
                matcher = loadMatcher(context),
                type = "com.credman.IdentityCredential",
                requestType = "",
                protocolTypes = emptyList(),
            )
        }

        private fun loadMatcher(context: Context): ByteArray =
            context.assets.open("dcapimatcher.wasm").use { stream ->
                ByteArray(stream.available()).apply {
                    stream.read(this)
                }
            }

        private fun getIconBytes(image: ByteArray?, androidPlatformAdapter: AndroidPlatformAdapter): ByteArrayOutputStream {
            val icon = image?.let { androidPlatformAdapter.decodeImage(it).getOrNull()?.asAndroidBitmap() }
            val scaledIcon = icon?.scale(128, 128)
            val stream = ByteArrayOutputStream()
            scaledIcon?.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream
        }
    }
}
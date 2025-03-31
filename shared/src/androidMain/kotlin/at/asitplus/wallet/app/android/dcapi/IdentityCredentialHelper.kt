package at.asitplus.wallet.app.android.dcapi

import AndroidPlatformAdapter
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import at.asitplus.wallet.app.common.decodeImage
import com.google.android.gms.identitycredentials.RegistrationRequest
import at.asitplus.wallet.app.common.dcapi.CredentialsContainer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.collections.forEach
import kotlin.text.toByteArray

// adapted from https://github.com/openwallet-foundation-labs/identity-credential/blob/d7a37a5c672ed6fe1d863cbaeb1a998314d19fc5/appholder/src/main/java/com/android/identity/wallet/credman/IdentityCredentialRegistry.kt
class IdentityCredentialHelper(
    private val credentialsContainer: CredentialsContainer,
    private val androidPlatformAdapter: AndroidPlatformAdapter,
) {
    fun toRegistrationRequest(context: Context): RegistrationRequest {
        return RegistrationRequest(
            credentials = credentialBytes(),
            matcher = loadMatcher(context),
            type = "com.credman.IdentityCredential",
            requestType = "",
            protocolTypes = emptyList(),
        )
    }

    private fun loadMatcher(context: Context): ByteArray {
        val stream = context.assets.open("identitycredentialmatcher.wasm")
        val matcher = ByteArray(stream.available())
        stream.read(matcher)
        stream.close()
        return matcher
    }

    private fun getIconBytes(image: ByteArray?): ByteArrayOutputStream {
        val icon = image?.let { androidPlatformAdapter.decodeImage(it).asAndroidBitmap() }
        val scaledIcon = icon?.let { Bitmap.createScaledBitmap(it, 128, 128, true) }
        val stream = ByteArrayOutputStream()
        scaledIcon?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream
    }

    private fun credentialBytes(): ByteArray {
        val icons = ByteArrayOutputStream()
        val iconSizeList = mutableListOf<Int>()
        val entries = credentialsContainer.credentials
        entries.forEach { entry ->
            val iconBytes = getIconBytes(entry.icon) //TODO what to do on empty image
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
    }
}
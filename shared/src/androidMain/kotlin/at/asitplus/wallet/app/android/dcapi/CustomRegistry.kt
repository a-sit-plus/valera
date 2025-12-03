package at.asitplus.wallet.app.android.dcapi

import android.content.Context
import androidx.credentials.registry.provider.RegistryManager
import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialRegistry

class CustomRegistry(
    credentialsCbor: ByteArray,
    context: Context,
    intentAction: String = RegistryManager.ACTION_GET_CREDENTIAL
) : DigitalCredentialRegistry(
    id = context.packageName,
    credentials = credentialsCbor,
    matcher = loadMatcher(context),
    intentAction = intentAction,
) {

    private companion object {
        private fun loadMatcher(context: Context): ByteArray =
            context.assets.open("dcapimatcher.wasm").use { stream ->
                ByteArray(stream.available()).apply {
                    stream.read(this)
                }
            }
    }
}
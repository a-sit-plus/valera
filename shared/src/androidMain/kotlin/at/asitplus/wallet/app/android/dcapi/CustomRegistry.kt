package at.asitplus.wallet.app.android.dcapi

import android.content.Context
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.registry.provider.RegisterCreationOptionsRequest
import androidx.credentials.registry.provider.RegistryManager
import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialRegistry
import io.github.aakira.napier.Napier

class CustomRegistry(
    credentialsCbor: ByteArray,
    context: Context,
    intentAction: String = RegistryManager.ACTION_GET_CREDENTIAL
) : DigitalCredentialRegistry(
    id = context.packageName,
    credentials = credentialsCbor,
    matcher = loadVerificationMatcher(context),
    intentAction = intentAction,
) {

    companion object {
        private const val ISSUANCE_REGISTRY_ID = "openid4vci"

        private fun loadVerificationMatcher(context: Context): ByteArray =
            context.assets.open("dcapimatcher.wasm").use { stream ->
                ByteArray(stream.available()).apply {
                    stream.read(this)
                }
            }

        private fun loadIssuanceMatcher(context: Context): ByteArray =
            context.assets.open("dcapimatcher_issuing.wasm").use { stream ->
                ByteArray(stream.available()).apply {
                    stream.read(this)
                }
            }

        @OptIn(ExperimentalDigitalCredentialApi::class)
        suspend fun registerIssuance(context: Context) {
            val request = object : RegisterCreationOptionsRequest(
                type = DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
                id = ISSUANCE_REGISTRY_ID,
                creationOptions = ByteArray(0),
                matcher = loadIssuanceMatcher(context),
                intentAction = RegistryManager.ACTION_CREATE_CREDENTIAL
            ) {}
            try {
                RegistryManager.create(context).registerCreationOptions(request)
                Napier.i("DC API: Issuance registration succeeded")
            } catch (e: Throwable) {
                Napier.w("DC API: Issuance registration failed", e)
            }
        }
    }
}

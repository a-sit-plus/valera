package at.asitplus.wallet.app.android.dcapi

import android.content.Context
import com.google.android.gms.identitycredentials.RegistrationRequest
import androidx.credentials.DigitalCredential.Companion.TYPE_DIGITAL_CREDENTIAL
import androidx.credentials.ExperimentalDigitalCredentialApi

class DCAPIAndroidExporter(private val context: Context) {

    @OptIn(ExperimentalDigitalCredentialApi::class)
    fun createRegistrationRequest(
        credentialsListCbor: ByteArray
    ): RegistrationRequest {
        return RegistrationRequest(
            credentials = credentialsListCbor,
            matcher = loadMatcher(),
            type = TYPE_DIGITAL_CREDENTIAL,
            requestType = "",
            protocolTypes = emptyList(),
        )
    }

    private fun loadMatcher(): ByteArray = context.assets.open("dcapimatcher.wasm").use { stream ->
        ByteArray(stream.available()).apply {
            stream.read(this)
        }
    }
}
package at.asitplus.wallet.app.android

import androidx.credentials.CustomCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import android.os.Bundle


/**
 * Represents the user's digital credential, generally used for verification or sign-in purposes.
 * Based on the androidx.credentials.DigitalCredential
 *
 * @property credential the digital credential
 */
@ExperimentalDigitalCredentialApi
class EncryptedDigitalCredential
private constructor(
    val credential: String,
    data: Bundle,
) : CustomCredential(TYPE_DIGITAL_CREDENTIAL, data) {

    init {
        require(credential.isNotEmpty()) {
            "credential must not be empty"
        }
    }

    /**
     * Constructs an `EncryptedDigitalCredential`.
     *
     * @param credential the digital credential
     * @throws IllegalArgumentException if the `credential` is empty
     */
    constructor(
        credential: String,
    ) : this(credential, toBundle(credential))

    /** Companion constants / helpers for [EncryptedDigitalCredential]. */
    companion object {
        /** The type value for public key credential related operations. */
        const val TYPE_DIGITAL_CREDENTIAL: String = "androidx.credentials.TYPE_DIGITAL_CREDENTIAL"

        internal const val BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"

        @JvmStatic
        internal fun createFrom(data: Bundle): EncryptedDigitalCredential {
            try {
                TODO("lkjewf")
                val credentialJson = data.getString("androidx.credentials.provider.extra.EXTRA_CREDENTIAL_DATA")
                return EncryptedDigitalCredential(credentialJson!!, data)
            } catch (e: Exception) {
                throw IllegalArgumentException("cannot create from bundle")
            }
        }

        @JvmStatic
        internal fun toBundle(response: String): Bundle {
            val bundle = Bundle()
            // "androidx.credentials.provider.extra.EXTRA_CREDENTIAL_DATA"
            println("hruwrewuirewhireghreiugreg")
            bundle.putString(BUNDLE_KEY_REQUEST_JSON, response)
            //bundle.putString("androidx.credentials.provider.extra.EXTRA_CREDENTIAL_DATA", response)
            return bundle
        }
    }
}

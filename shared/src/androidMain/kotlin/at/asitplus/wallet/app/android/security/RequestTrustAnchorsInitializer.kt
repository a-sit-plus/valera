package at.asitplus.wallet.app.android.security

import android.content.Context
import at.asitplus.wallet.app.common.domain.requestcertificates.RequestCertificateTrustAnchors
import io.github.aakira.napier.Napier

internal class RequestTrustAnchorsInitializer {
    fun initialize(context: Context): Boolean {
        val loaded = loadRequestTrustAnchorsFromLocalPem(context)
        if (!loaded) {
            Napier.e("Could not initialize trusted request root certificates from local PEM.")
        }
        return loaded
    }

    private fun loadRequestTrustAnchorsFromLocalPem(context: Context): Boolean =
        runCatching {
            val trustedRootPem = context.assets.open(TRUST_ANCHORS_ASSET_PATH).use { stream ->
                stream.readBytes().decodeToString()
            }
            val replaced = RequestCertificateTrustAnchors.replaceFromPem(trustedRootPem)
            if (replaced) {
                Napier.i("Loaded trusted request root certificates from assets/$TRUST_ANCHORS_ASSET_PATH")
            } else {
                Napier.e("Trusted request root certificates from assets/$TRUST_ANCHORS_ASSET_PATH are invalid")
            }
            replaced
        }.getOrElse {
            Napier.e("Could not load trusted request root certificates from assets/$TRUST_ANCHORS_ASSET_PATH", it)
            false
        }

    private companion object {
        const val TRUST_ANCHORS_ASSET_PATH = "trust/msca-root.pem"
    }
}

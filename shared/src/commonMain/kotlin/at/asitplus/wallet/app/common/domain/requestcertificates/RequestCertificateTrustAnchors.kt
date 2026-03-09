package at.asitplus.wallet.app.common.domain.requestcertificates

import at.asitplus.signum.indispensable.pki.X509Certificate
import io.github.aakira.napier.Napier

private val certificatePemPattern =
    Regex("-----BEGIN CERTIFICATE-----[\\s\\S]*?-----END CERTIFICATE-----")

internal object RequestCertificateTrustAnchors {
    private var trustedRoots: List<X509Certificate> = emptyList()

    fun replaceFromPem(pemContent: String): Boolean {
        val parsed = runCatching {
            certificatePemPattern.findAll(pemContent)
                .map { it.value.trim() }
                .mapIndexedNotNull { index, certPem ->
                    X509Certificate.decodeFromPem(certPem).getOrElse {
                        Napier.e("Could not parse trusted root certificate at index $index", it)
                        return@mapIndexedNotNull null
                    }
                }
                .toList()
        }.getOrElse {
            Napier.e("Could not parse trusted root PEM content.", it)
            emptyList()
        }

        if (parsed.isEmpty()) {
            Napier.e("No certificates found in trusted root PEM content.")
            return false
        }

        trustedRoots = parsed
        return true
    }

    fun current(): List<X509Certificate> = trustedRoots
}

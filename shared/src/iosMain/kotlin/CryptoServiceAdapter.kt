import at.asitplus.signum.indispensable.CryptoPublicKey
import at.asitplus.signum.indispensable.X509SignatureAlgorithm
import at.asitplus.signum.indispensable.cosef.CoseKey
import at.asitplus.signum.indispensable.josef.JsonWebKey
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.app.common.WalletCryptoService
import at.asitplus.wallet.lib.agent.KeyPairAdapter
import at.asitplus.wallet.lib.agent.generateSelfSignedCertificate

abstract class CryptoServiceAdapter(
    val publicKey: CryptoPublicKey,
    val algorithm: X509SignatureAlgorithm,
    val coseKey: CoseKey,
    val jsonWebKey: JsonWebKey,
    private val certificate: X509Certificate?
) : WalletCryptoService() {
    override val keyPairAdapter: KeyPairAdapter
        get() = object : KeyPairAdapter {
            override val publicKey: CryptoPublicKey
                get() = this@CryptoServiceAdapter.publicKey

            override val certificate: X509Certificate?
                    by lazy {
                        this@CryptoServiceAdapter.certificate ?: run {
                            X509Certificate.generateSelfSignedCertificate(
                                publicKey,
                                X509SignatureAlgorithm.ES256
                            ) { sign(it) }
                        }
                    }

            override val coseKey: CoseKey
                get() = this@CryptoServiceAdapter.coseKey

            override val identifier: String
                get() = publicKey.didEncoded

            override val jsonWebKey: JsonWebKey
                get() = this@CryptoServiceAdapter.jsonWebKey

            override val signingAlgorithm: X509SignatureAlgorithm
                get() = this@CryptoServiceAdapter.algorithm
        }
}

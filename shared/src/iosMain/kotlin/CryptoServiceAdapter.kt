import at.asitplus.signum.indispensable.CryptoPublicKey
import at.asitplus.signum.indispensable.X509SignatureAlgorithm
import at.asitplus.signum.indispensable.cosef.CoseKey
import at.asitplus.signum.indispensable.josef.JsonWebKey
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.app.common.WalletCryptoService

abstract class CryptoServiceAdapter(
    override val publicKey: CryptoPublicKey,
    override val algorithm: X509SignatureAlgorithm,
    override val coseKey: CoseKey,
    override val jsonWebKey: JsonWebKey,
    override val certificate: X509Certificate?
) : WalletCryptoService
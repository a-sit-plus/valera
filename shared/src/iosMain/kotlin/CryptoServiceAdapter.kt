import at.asitplus.crypto.datatypes.CryptoAlgorithm
import at.asitplus.crypto.datatypes.CryptoPublicKey
import at.asitplus.crypto.datatypes.X509SignatureAlgorithm
import at.asitplus.crypto.datatypes.cose.CoseKey
import at.asitplus.crypto.datatypes.jws.JsonWebKey
import at.asitplus.crypto.datatypes.pki.X509Certificate
import at.asitplus.wallet.lib.agent.CryptoService

abstract class CryptoServiceAdapter(
    override val publicKey: CryptoPublicKey,
    override val algorithm: X509SignatureAlgorithm,
    override val coseKey: CoseKey,
    override val jsonWebKey: JsonWebKey,
    override val certificate: X509Certificate?
) : CryptoService
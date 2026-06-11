package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.signum.supreme.sign.Signer

internal expect object WalletPlatformKeyStore {
    suspend fun getSignerForKey(alias: String): KmmResult<Signer>
    suspend fun createBindingKey(alias: String): KmmResult<Signer>
    suspend fun createCapabilityKey(alias: String): KmmResult<Signer>
    suspend fun createAttestationKey(alias: String, challenge: ByteArray): KmmResult<Signer>
    suspend fun deleteSigningKey(alias: String): KmmResult<Unit>
}

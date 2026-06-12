package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.signum.indispensable.ECCurve
import at.asitplus.signum.supreme.dsl.PREFERRED
import at.asitplus.signum.supreme.os.PlatformSigningProvider
import at.asitplus.signum.supreme.sign.Signer

internal actual object WalletPlatformKeyStore {
    actual suspend fun getSignerForKey(alias: String): KmmResult<Signer> = catching {
        PlatformSigningProvider.getSignerForKey(alias).getOrThrow()
    }

    actual suspend fun createBindingKey(alias: String): KmmResult<Signer> = catching {
        PlatformSigningProvider.createSigningKey(alias = alias) {
            ec {
                curve = ECCurve.SECP_256_R_1
                purposes {
                    keyAgreement = true
                    signing = true
                }
            }
            hardware {
                backing = PREFERRED
                protection {
                    factors {
                        biometry = true
                    }
                    timeout = Configuration.BIOMETRIC_TIMEOUT
                }
            }
        }.getOrThrow()
    }

    actual suspend fun createCapabilityKey(alias: String): KmmResult<Signer> = catching {
        PlatformSigningProvider.createSigningKey(alias).getOrThrow()
    }

    actual suspend fun createAttestationKey(alias: String, challenge: ByteArray): KmmResult<Signer> = catching {
        PlatformSigningProvider.createSigningKey(alias) {
            ec {}
            hardware {
                attestation {
                    this.challenge = challenge
                }
            }
        }.getOrThrow()
    }

    actual suspend fun deleteSigningKey(alias: String): KmmResult<Unit> =
        PlatformSigningProvider.deleteSigningKey(alias)

    actual suspend fun deleteLegacySigningKeyIfPresent(alias: String): KmmResult<Boolean> =
        KmmResult.success(false)
}

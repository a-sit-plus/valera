package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.signum.indispensable.ECCurve
import at.asitplus.signum.supreme.dsl.PREFERRED
import at.asitplus.signum.supreme.os.IosKeychainProvider
import at.asitplus.signum.supreme.os.IosSignerConfiguration
import at.asitplus.signum.supreme.os.IosSigningKeyConfiguration
import at.asitplus.signum.supreme.sign.Signer
import platform.Foundation.NSBundle

private const val keychainAccessGroupInfoPlistKey = "WalletKeychainAccessGroupIdentifier"
private const val keychainTagOverrideInfoPlistKey = "WalletKeychainTagOverride"

private data class WalletIosKeychainConfig(
    val accessGroup: String,
    val tagOverride: String
)

private val walletIosKeychainConfig by lazy {
    WalletIosKeychainConfig(
        accessGroup = resolvedInfoPlistString(keychainAccessGroupInfoPlistKey),
        tagOverride = resolvedInfoPlistString(keychainTagOverrideInfoPlistKey)
    )
}

private fun resolvedInfoPlistString(key: String): String {
    val value = NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String
    require(!value.isNullOrBlank() && "\$(" !in value) {
        "Could not resolve $key from Info.plist. Configure KEYCHAIN_ACCESS_GROUP_IDENTIFIER " +
            "and APP_BUNDLE_IDENTIFIER in iosApp/Configuration/Config.xcconfig or Signing.local.xcconfig."
    }
    return value
}

private fun IosSignerConfiguration.useWalletKeychain() {
    val config = walletIosKeychainConfig
    accessGroup = config.accessGroup
    tagOverride = config.tagOverride
}

private fun IosSigningKeyConfiguration.useWalletKeychain() {
    val config = walletIosKeychainConfig
    accessGroup = config.accessGroup
    tagOverride = config.tagOverride
    signer {
        useWalletKeychain()
    }
}

internal actual object WalletPlatformKeyStore {
    actual suspend fun getSignerForKey(alias: String): KmmResult<Signer> = catching {
        IosKeychainProvider.getSignerForKey(alias) {
            useWalletKeychain()
        }.getOrThrow()
    }

    actual suspend fun createBindingKey(alias: String): KmmResult<Signer> = catching {
        IosKeychainProvider.createSigningKey(alias = alias) {
            useWalletKeychain()
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
        IosKeychainProvider.createSigningKey(alias) {
            useWalletKeychain()
        }.getOrThrow()
    }

    actual suspend fun createAttestationKey(alias: String, challenge: ByteArray): KmmResult<Signer> = catching {
        IosKeychainProvider.createSigningKey(alias) {
            useWalletKeychain()
            ec {}
            hardware {
                attestation {
                    this.challenge = challenge
                }
            }
        }.getOrThrow()
    }

    actual suspend fun deleteSigningKey(alias: String): KmmResult<Unit> =
        IosKeychainProvider.deleteSigningKey(alias) {
            useWalletKeychain()
        }

    actual suspend fun deleteLegacySigningKeyIfPresent(alias: String): KmmResult<Boolean> = catching {
        val legacySignerExists = IosKeychainProvider.getSignerForKey(alias).isSuccess
        if (legacySignerExists) {
            IosKeychainProvider.deleteSigningKey(alias).getOrThrow()
        }
        legacySignerExists
    }
}

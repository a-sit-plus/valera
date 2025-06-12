package at.asitplus.wallet.app.common


import kotlin.time.Duration.Companion.seconds

object Configuration {
    val USER_AUTHENTICATION_TIMEOUT_SECONDS = 15

    const val DATASTORE_KEY_CONFIG = "config"
    const val DATASTORE_KEY_VCS = "VCs"
    const val DATASTORE_KEY_PROVISIONING_CONTEXT = "provisioning_context"
    const val DATASTORE_KEY_COOKIES = "cookies"
    const val DATASTORE_SIGNING_CONFIG = "signingConfig"
    const val DEBUG_DATASTORE_KEY = "DBGKEY"
    const val DEBUG_DATASTORE_VALUE = "DBGVALUE"
    const val KS_ALIAS_OLD = "wallet-supreme-binding-key"
    const val KS_ALIAS = "wallet-binding-sig-enc-key"
    val BIOMETRIC_TIMEOUT = 15.seconds
}
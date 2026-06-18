package at.asitplus.wallet.app.common


import kotlin.time.Duration.Companion.seconds

object Configuration {
    val USER_AUTHENTICATION_TIMEOUT_SECONDS = 15

    const val DATASTORE_KEY_CONFIG = "config"
    const val DATASTORE_KEY_VCS = "VCs"
    const val DATASTORE_KEY_CREDENTIAL_METADATA_CACHE = "credential_metadata_cache"
    const val DATASTORE_KEY_REFRESH_SUPPRESSED_CREDENTIALS = "refresh_suppressed_credentials"
    const val DATASTORE_KEY_PROVISIONING_CONTEXT_BY_STATE = "provisioning_context_by_state"
    const val DATASTORE_KEY_PROVISIONING_INSTANCE_ATTESTATION_BY_STATE = "provisioning_instance_attestation_by_state"
    const val DATASTORE_KEY_ACTIVE_PROVISIONING_STATE = "active_provisioning_state"
    const val DATASTORE_KEY_PROVISIONING_STATE_TO_CODE_STORE = "provisioning_state_to_code_store"
    const val DATASTORE_KEY_COOKIES = "cookies"
    const val DATASTORE_SIGNING_CONFIG = "signingConfig"
    const val DATASTORE_CAPABILITIES_ATTESTATION = "capabilitiesAttestation"
    const val DEBUG_DATASTORE_KEY = "DBGKEY"
    const val DEBUG_DATASTORE_VALUE = "DBGVALUE"
    val KS_ALIASES_OLDER_THAN_THE_HILLS = platformKsAliasOld
    val KS_ALIAS = platformKsAlias
    const val KS_CAPABILITY_ALIAS = "wallet-capabilities-key"
    val BIOMETRIC_TIMEOUT = 15.seconds
}

internal expect val platformKsAliasOld: Array<String>
internal expect val platformKsAlias: String

package at.asitplus.wallet.app.common


import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object Configuration {
    val USER_AUTHENTICATION_TIMEOUT_SECONDS = 15

    const val DATASTORE_KEY_CONFIG = "config"
    const val DATASTORE_KEY_VCS = "VCs"
    const val DATASTORE_KEY_CREDENTIAL_METADATA_CACHE = "credential_metadata_cache"
    const val DATASTORE_KEY_TOKEN_STATUS_LIST_CACHE = "token_status_list_cache"
    const val MAX_PERSISTENT_CACHE_ENTRIES = 64

    // Raw HTTP responses (validators + body) shared by all public remote resources.
    const val DATASTORE_KEY_HTTP_CACHE = "http_cache"

    // Offline-TTL per remote resource: how long a cached copy is served before a network refresh is required.
    val CACHE_TTL_CREDENTIAL_METADATA: Duration = 7.days
    val CACHE_TTL_TOKEN_STATUS: Duration = 1.days
    val CACHE_TTL_TRUST_LIST: Duration = 7.days

    // How long a DC API request waits for missing or expired Trust Lists before it is validated with cached ones.
    val TRUST_LIST_REFRESH_TIMEOUT: Duration = 3.seconds

    // After any cached-resource GET returns an error status (e.g. 404/503), suppress retries for that URL
    // this long (in-memory). Stops broken endpoints being re-hit on every freshness check / UI visit.
    val HTTP_FAILURE_COOLDOWN: Duration = 1.hours

    // After a cached-resource GET got no response (offline, DNS, timeout) or an unusable one (e.g. an invalid status
    // list token), suppress retries for that URL this long, as every check would wait for it to fail again otherwise.
    val HTTP_TRANSIENT_FAILURE_COOLDOWN: Duration = 5.minutes

    // Timeouts of the cached-resource client, so that an unreachable host fails a check instead of blocking it for
    // the engine's default. No total request timeout, as a large Trust List may take long on a slow connection.
    val HTTP_CONNECT_TIMEOUT: Duration = 10.seconds
    val HTTP_SOCKET_TIMEOUT: Duration = 15.seconds
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

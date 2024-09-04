package at.asitplus.wallet.app.common

object Configuration {
    val BUILD_FOR_STAGE = BuildEnvironment.Development.abbreviation
    val USER_AUTHENTICATION_TIMEOUT_SECONDS = 15

    const val DATASTORE_KEY_CONFIG = "config"
    const val DATASTORE_KEY_VCS = "VCs"
    const val DATASTORE_KEY_PROVISIONING_CONTEXT = "provisioning_context"
    const val DATASTORE_KEY_COOKIES = "cookies"
    const val DEBUG_DATASTORE_KEY = "DBGKEY"
    const val DEBUG_DATASTORE_VALUE = "DBGVALUE"

    const val IOS_TEST_VALUE = "TESTVALUE"

    const val KS_ALIAS = "supreme-binding"
}
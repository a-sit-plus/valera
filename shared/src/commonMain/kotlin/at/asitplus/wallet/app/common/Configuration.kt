package at.asitplus.wallet.app.common

import BuildEnvironment

object Configuration {
    val BUILD_FOR_STAGE = BuildEnvironment.Development.abbreviation
    val USER_AUTHENTICATION_TIMEOUT = 5 * 60

    const val DATASTORE_KEY_VCS = "VCs"
    const val DATASTORE_KEY_XAUTH = "xauth"
    const val DATASTORE_KEY_COOKIES = "cookies"
    const val DEBUG_DATASTORE_KEY = "DBGKEY"
    const val DEBUG_DATASTORE_VALUE = "DBGVALUE"
    const val DATASTORE_KEY_CONFIG = "config"

    const val IOS_TEST_VALUE = "TESTVALUE"
}
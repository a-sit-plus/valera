package at.asitplus.wallet.app.common

import platform.LocalAuthentication.LAContext

data class IosCryptoServiceAuthorizationContext(
    val contex: LAContext,
    val reason: String,
): CryptoServiceAuthorizationContext
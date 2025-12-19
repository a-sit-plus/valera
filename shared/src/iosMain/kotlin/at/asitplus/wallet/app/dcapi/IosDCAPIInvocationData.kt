package at.asitplus.wallet.app.dcapi

import at.asitplus.wallet.app.common.dcapi.DCAPIInvocationData
import platform.Foundation.NSData

data class IosDCAPIInvocationData(
    val rawRequest: String?,
    val origin: String?,
    val onFinish: (NSData?) -> Unit
) : DCAPIInvocationData

package at.asitplus.wallet.app.dcapi

data class IosDcApiPreRequestData(
    val parsedRequestSummary: String?,
    val origin: String?,
    val onContinue: () -> Unit,
    val onCancel: () -> Unit,
)

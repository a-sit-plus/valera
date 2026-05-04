package at.asitplus.wallet.app.common.dcapi

data class DCAPIIssuingRequest(
    val requestJson: String,
    val callingPackageName: String,
    val callingOrigin: String
)

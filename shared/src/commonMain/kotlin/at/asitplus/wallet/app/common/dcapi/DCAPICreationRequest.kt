package at.asitplus.wallet.app.common.dcapi

data class DCAPICreationRequest(
    val requestJson: String,
    val callingPackageName: String,
    val callingOrigin: String
)

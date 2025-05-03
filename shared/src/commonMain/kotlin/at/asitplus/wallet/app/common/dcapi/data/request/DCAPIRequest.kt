package at.asitplus.wallet.app.common.dcapi.data.request

sealed class DCAPIRequest {

    abstract fun serialize(): String
}
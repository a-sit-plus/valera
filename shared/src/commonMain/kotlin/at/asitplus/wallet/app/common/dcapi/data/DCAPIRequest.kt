package at.asitplus.wallet.app.common.dcapi.data

abstract class DCAPIRequest {

    abstract fun serialize(): String
}
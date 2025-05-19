package at.asitplus.wallet.app.common.dcapi.data

import at.asitplus.wallet.lib.data.vckJsonSerializer

class ErrorResponse(val error: String) {
    fun serialize() = vckJsonSerializer.encodeToString(this)
}

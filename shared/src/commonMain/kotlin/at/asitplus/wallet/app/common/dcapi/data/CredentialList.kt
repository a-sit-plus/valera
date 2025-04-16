package at.asitplus.wallet.app.common.dcapi.data

import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import kotlinx.serialization.encodeToByteArray

data class CredentialList(
    val entries: List<CredentialEntry>
) {
    fun serialize(): ByteArray = coseCompliantSerializer.encodeToByteArray(entries)
}
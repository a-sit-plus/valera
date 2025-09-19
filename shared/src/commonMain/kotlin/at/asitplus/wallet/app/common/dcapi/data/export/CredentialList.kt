package at.asitplus.wallet.app.common.dcapi.data.export

import kotlinx.serialization.Serializable

@Serializable
data class CredentialList(
    val entries: List<CredentialEntry>
)
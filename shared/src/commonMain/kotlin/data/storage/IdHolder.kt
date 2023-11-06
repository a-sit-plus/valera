package data.storage
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import kotlinx.serialization.Serializable

@Serializable
data class IdHolder(
    val credentials: ArrayList<VerifiableCredentialJws> = arrayListOf()
)
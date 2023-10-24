package data.storeage
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import kotlinx.serialization.Serializable
import kotlin.jvm.Transient

@Serializable
data class IdHolder(
    val credentials: ArrayList<VerifiableCredentialJws> = arrayListOf()
)
package data.storage
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import at.asitplus.wallet.lib.iso.IssuerSigned
import kotlinx.serialization.Serializable

@Serializable
data class IdHolder(
    val credentialsVcJws: ArrayList<VerifiableCredentialJws> = arrayListOf(),
    val credentialsSdJwt: ArrayList<VerifiableCredentialSdJwt> = arrayListOf(),
    val credentialsIso: ArrayList<IssuerSigned> = arrayListOf()
)
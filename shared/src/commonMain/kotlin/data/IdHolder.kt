package data
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import kotlinx.serialization.Serializable
import kotlin.jvm.Transient

@Serializable
data class IdHolder(
    val id: String, val credentials: ArrayList<IdVc> = arrayListOf(),
    val attachments: HashMap<String, ByteArray> = hashMapOf()
) {
    fun vcIdExist(vcId: String): Boolean{
        val list = credentials.map { it.vcJwsSerialized }
            .map { VerifiableCredentialJws.deserialize(it) }
        for (it in list){
            if (it?.jwtId == vcId){
                return true
            }
        }
        return false
    }

    @Transient
    @kotlinx.serialization.Transient
    val revocationListUrl: String? = credentials
        .map { it.vcJwsSerialized }
        .map { VerifiableCredentialJws.deserialize(it) }
        .map { it?.vc?.credentialStatus?.statusListUrl }
        .firstOrNull()

    @Transient
    @kotlinx.serialization.Transient
    val revocationListUrlWithIndex: String? = credentials
        .map { it.vcJwsSerialized }
        .map { VerifiableCredentialJws.deserialize(it) }
        .map { it?.vc?.credentialStatus?.id }
        .firstOrNull()
}

@Serializable
data class IdVc(
    val attrName: String,
    val attrTypes: Array<String>,
    val vcSerialized: String,
    val vcJwsSerialized: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IdVc

        if (attrName != other.attrName) return false
        if (!attrTypes.contentEquals(other.attrTypes)) return false
        if (vcSerialized != other.vcSerialized) return false
        if (vcJwsSerialized != other.vcJwsSerialized) return false

        return true
    }

    override fun hashCode(): Int {
        var result = attrName.hashCode()
        result = 31 * result + attrTypes.contentHashCode()
        result = 31 * result + vcSerialized.hashCode()
        result = 31 * result + vcJwsSerialized.hashCode()
        return result
    }
}
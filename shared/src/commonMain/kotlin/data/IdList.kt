package data

import at.asitplus.wallet.lib.data.jsonSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString

@Serializable
data class IdList(val idHolders: ArrayList<IdHolder>) {

    fun serialize() = jsonSerializer.encodeToString(this)

    fun size(): Int {
        return idHolders.size
    }

    fun isNotEmpty(): Boolean {
        return idHolders.isNotEmpty()
    }

    fun getOrCreate(id: String): IdHolder {
        return idHolders.find { credentials -> credentials.id == id } ?:
        IdHolder(id, idHolders.size.toLong()).also { creds -> idHolders.add(creds) }
    }

    operator fun get(id: String) : IdHolder? {
        return idHolders.find { credentials -> credentials.id == id }
    }

    @Transient
    val revocationListUrl: String? = idHolders.map { it.revocationListUrl }.firstOrNull()

    companion object {
        fun deserialize(it: String) = jsonSerializer.decodeFromString<IdList>(it)
    }
}



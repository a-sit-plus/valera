package at.asitplus.wallet.idaustria

import at.asitplus.wallet.lib.data.CredentialSubject
import at.asitplus.wallet.lib.jws.ByteArrayBase64UrlSerializer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.serializers.LocalDateIso8601Serializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("IdAustria2023")
class IdAustriaCredential : CredentialSubject {
    @SerialName("firstname")
    val firstname: String

    @SerialName("lastname")
    val lastname: String

    @SerialName("date-of-birth")
    @Serializable(with = LocalDateIso8601Serializer::class)
    val dateOfBirth: LocalDate

    @SerialName("portrait")
    @Serializable(with = ByteArrayBase64UrlSerializer::class)
    val portrait: ByteArray?

    constructor(id: String, firstname: String, lastname: String, dateOfBirth: LocalDate, portrait: ByteArray?) : super(id = id) {
        this.firstname = firstname
        this.lastname = lastname
        this.dateOfBirth = dateOfBirth
        this.portrait = portrait
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IdAustriaCredential

        if (id != other.id) return false
        if (firstname != other.firstname) return false
        if (lastname != other.lastname) return false
        if (dateOfBirth != other.dateOfBirth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + firstname.hashCode()
        result = 31 * result + lastname.hashCode()
        result = 31 * result + dateOfBirth.hashCode()
        return result
    }

}

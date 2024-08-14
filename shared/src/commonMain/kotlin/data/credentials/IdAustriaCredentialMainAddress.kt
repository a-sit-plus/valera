package data.credentials

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IdAustriaCredentialMainAddress(
    @SerialName("Gemeindekennziffer")
    val municipalityCode: String? = null,
    @SerialName("Gemeindebezeichnung")
    val municipalityName: String? = null,
    @SerialName("Postleitzahl")
    val postalCode: String? = null,
    @SerialName("Ortschaft")
    val locality: String? = null,
    @SerialName("Strasse")
    val street: String? = null,
    @SerialName("Hausnummer")
    val doorNumber: String? = null,
    @SerialName("Stiege")
    val stair: String? = null,
    @SerialName("Tuer")
    val door: String? = null,
)
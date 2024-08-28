package data.credentials

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IdAustriaCredentialMainAddress(
    @SerialName(GEMEINDEKENNZIFFER)
    val municipalityCode: String? = null,
    @SerialName(GEMEINDEBEZEICHNUNG)
    val municipalityName: String? = null,
    @SerialName(POSTLEITZAHL)
    val postalCode: String? = null,
    @SerialName(ORTSCHAFT)
    val locality: String? = null,
    @SerialName(STRASSE)
    val street: String? = null,
    @SerialName(HAUSNUMMER)
    val houseNumber: String? = null,
    @SerialName(STIEGE)
    val stair: String? = null,
    @SerialName(TUER)
    val door: String? = null,
) {
    companion object {
        const val GEMEINDEKENNZIFFER = "Gemeindekennziffer"
        const val GEMEINDEBEZEICHNUNG = "Gemeindebezeichnung"
        const val POSTLEITZAHL = "Postleitzahl"
        const val ORTSCHAFT = "Ortschaft"
        const val STRASSE = "Strasse"
        const val HAUSNUMMER = "Hausnummer"
        const val STIEGE = "Stiege"
        const val TUER = "Tuer"
    }
}
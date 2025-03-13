package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_14
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_16
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_18
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_21
import at.asitplus.valera.resources.attribute_friendly_name_bpk
import at.asitplus.valera.resources.attribute_friendly_name_date_of_birth
import at.asitplus.valera.resources.attribute_friendly_name_firstname
import at.asitplus.valera.resources.attribute_friendly_name_lastname
import at.asitplus.valera.resources.attribute_friendly_name_main_address
import at.asitplus.valera.resources.attribute_friendly_name_portrait
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_door
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_house_number
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_location
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_municipality_code
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_municipality_name
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_postal_code
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_stair
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_street
import at.asitplus.wallet.idaustria.IdAustriaScheme

object IdAustriaCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath) =
        when (val first = attributeName.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                IdAustriaScheme.Attributes.BPK -> Res.string.attribute_friendly_name_bpk
                IdAustriaScheme.Attributes.FIRSTNAME -> Res.string.attribute_friendly_name_firstname
                IdAustriaScheme.Attributes.LASTNAME -> Res.string.attribute_friendly_name_lastname
                IdAustriaScheme.Attributes.DATE_OF_BIRTH -> Res.string.attribute_friendly_name_date_of_birth
                IdAustriaScheme.Attributes.PORTRAIT -> Res.string.attribute_friendly_name_portrait
                IdAustriaScheme.Attributes.AGE_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
                IdAustriaScheme.Attributes.AGE_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
                IdAustriaScheme.Attributes.AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
                IdAustriaScheme.Attributes.AGE_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
                IdAustriaScheme.Attributes.MAIN_ADDRESS -> {
                    when (val second = attributeName.segments.getOrNull(1)) {
                        is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                            IdAustriaCredentialMainAddress.GEMEINDEKENNZIFFER -> Res.string.id_austria_credential_attribute_friendly_name_main_address_municipality_code
                            IdAustriaCredentialMainAddress.GEMEINDEBEZEICHNUNG -> Res.string.id_austria_credential_attribute_friendly_name_main_address_municipality_name
                            IdAustriaCredentialMainAddress.POSTLEITZAHL -> Res.string.id_austria_credential_attribute_friendly_name_main_address_postal_code
                            IdAustriaCredentialMainAddress.ORTSCHAFT -> Res.string.id_austria_credential_attribute_friendly_name_main_address_location
                            IdAustriaCredentialMainAddress.STRASSE -> Res.string.id_austria_credential_attribute_friendly_name_main_address_street
                            IdAustriaCredentialMainAddress.HAUSNUMMER -> Res.string.id_austria_credential_attribute_friendly_name_main_address_house_number
                            IdAustriaCredentialMainAddress.STIEGE -> Res.string.id_austria_credential_attribute_friendly_name_main_address_stair
                            IdAustriaCredentialMainAddress.TUER -> Res.string.id_austria_credential_attribute_friendly_name_main_address_door
                            else -> null
                        }

                        null -> Res.string.attribute_friendly_name_main_address

                        else -> null
                    }
                }

                else -> null
            }

            else -> null
        }
}
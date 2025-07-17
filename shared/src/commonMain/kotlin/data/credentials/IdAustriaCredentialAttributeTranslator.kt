package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
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

class IdAustriaCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(
        attributeName: NormalizedJsonPath
    ) = IdAustriaCredentialSdJwtClaimDefinitionResolver().resolveOrNull(
        attributeName
    )?.stringResource()

    private fun IdAustriaCredentialClaimDefinition.stringResource() = when(this) {
        IdAustriaCredentialClaimDefinition.BPK -> Res.string.attribute_friendly_name_bpk
        IdAustriaCredentialClaimDefinition.FIRSTNAME -> Res.string.attribute_friendly_name_firstname
        IdAustriaCredentialClaimDefinition.LASTNAME -> Res.string.attribute_friendly_name_lastname
        IdAustriaCredentialClaimDefinition.DATE_OF_BIRTH -> Res.string.attribute_friendly_name_date_of_birth
        IdAustriaCredentialClaimDefinition.PORTRAIT -> Res.string.attribute_friendly_name_portrait
        IdAustriaCredentialClaimDefinition.AGE_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
        IdAustriaCredentialClaimDefinition.AGE_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
        IdAustriaCredentialClaimDefinition.AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
        IdAustriaCredentialClaimDefinition.AGE_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
        IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_CONTAINER ->  Res.string.attribute_friendly_name_main_address
        IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_GEMEINDEKENNZIFFER -> Res.string.id_austria_credential_attribute_friendly_name_main_address_municipality_code
        IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_GEMEINDEBEZEICHNUNG -> Res.string.id_austria_credential_attribute_friendly_name_main_address_municipality_name
        IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_POSTLEITZAHL -> Res.string.id_austria_credential_attribute_friendly_name_main_address_postal_code
        IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_ORTSCHAFT -> Res.string.id_austria_credential_attribute_friendly_name_main_address_location
        IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_STRASSE -> Res.string.id_austria_credential_attribute_friendly_name_main_address_street
        IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_HAUSNUMMER -> Res.string.id_austria_credential_attribute_friendly_name_main_address_house_number
        IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_STIEGE -> Res.string.id_austria_credential_attribute_friendly_name_main_address_stair
        IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_TUER -> Res.string.id_austria_credential_attribute_friendly_name_main_address_door
    }
}


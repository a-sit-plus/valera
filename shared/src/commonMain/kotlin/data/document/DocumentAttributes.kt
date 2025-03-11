package data.document

import at.asitplus.valera.resources.*
import data.document.DocType.AGE_VERIFICATION
import data.document.DocType.IDENTITY
import data.document.DocType.MDL_DOCTYPE
import data.document.Namespace.MDL_NAMESPACE
import org.jetbrains.compose.resources.StringResource

enum class DocumentAttributes(
    val value: String,
    val displayName: StringResource,
    val type: ValueType
) {
    PORTRAIT("portrait", Res.string.attribute_friendly_name_portrait, ValueType.IMAGE),
    AGE_OVER_14("age_over_14", Res.string.attribute_friendly_name_age_at_least_14, ValueType.BOOL),
    AGE_OVER_16("age_over_16", Res.string.attribute_friendly_name_age_at_least_16, ValueType.BOOL),
    AGE_OVER_18("age_over_18", Res.string.attribute_friendly_name_age_at_least_18, ValueType.BOOL),
    AGE_OVER_21("age_over_21", Res.string.attribute_friendly_name_age_at_least_21, ValueType.BOOL),
    GIVEN_NAME("given_name", Res.string.attribute_friendly_name_firstname, ValueType.STRING),
    FAMILY_NAME("family_name", Res.string.attribute_friendly_name_lastname, ValueType.STRING),
    SIGNATURE_USUAL_MARK("signature_usual_mark", Res.string.attribute_friendly_name_signature_usual_mark,
        ValueType.IMAGE
    ),
    BIRTH_DATE("birth_date", Res.string.attribute_friendly_name_date_of_birth, ValueType.DATE),
    FAMILY_NAME_BIRTH("family_name_birth", Res.string.attribute_friendly_name_family_name_birth,
        ValueType.STRING
    ),
    GIVEN_NAME_BIRTH("given_name_birth", Res.string.attribute_friendly_name_given_name_birth,
        ValueType.STRING
    ),
    BIRTH_PLACE("birth_place", Res.string.attribute_friendly_name_birth_place, ValueType.STRING),
    BIRTH_COUNTRY("birth_country", Res.string.attribute_friendly_name_birth_country,
        ValueType.STRING
    ),
    BIRTH_STATE("birth_state", Res.string.attribute_friendly_name_birth_state, ValueType.STRING),
    BIRTH_CITY("birth_city", Res.string.attribute_friendly_name_birth_city, ValueType.STRING),
    AGE_IN_YEARS("age_in_years", Res.string.attribute_friendly_name_age_in_years, ValueType.INT),
    AGE_BIRTH_YEAR("age_birth_year", Res.string.attribute_friendly_name_age_birth_year,
        ValueType.INT
    ),
    RESIDENT_ADDRESS("resident_address", Res.string.attribute_friendly_name_main_address,
        ValueType.STRING
    ),
    RESIDENT_COUNTRY("resident_country", Res.string.attribute_friendly_name_main_residence_country,
        ValueType.STRING
    ),
    RESIDENT_STATE("resident_state", Res.string.attribute_friendly_name_main_residence_state,
        ValueType.STRING
    ),
    RESIDENT_CITY("resident_city", Res.string.attribute_friendly_name_main_residence_city,
        ValueType.STRING
    ),
    RESIDENT_POSTAL_CODE("resident_postal_code", Res.string.attribute_friendly_name_main_residence_postal_code,
        ValueType.STRING
    ),
    RESIDENT_STREET("resident_street", Res.string.attribute_friendly_name_main_residence_street,
        ValueType.STRING
    ),
    RESIDENT_HOUSE_NUMBER("resident_house_number", Res.string.attribute_friendly_name_main_residence_house_number,
        ValueType.INT
    ),
    SEX("sex", Res.string.attribute_friendly_name_sex, ValueType.INT),
    NATIONALITY("nationality", Res.string.attribute_friendly_name_nationality, ValueType.STRING),
    ISSUANCE_DATE("issuance_date", Res.string.attribute_friendly_name_issue_date, ValueType.DATE),
    ISSUE_DATE("issue_date", Res.string.attribute_friendly_name_issue_date, ValueType.DATE),
    EXPIRY_DATE("expiry_date", Res.string.attribute_friendly_name_expiry_date, ValueType.DATE),
    ISSUING_AUTHORITY("issuing_authority", Res.string.attribute_friendly_name_issuing_authority,
        ValueType.STRING
    ),
    DOCUMENT_NUMBER("document_number", Res.string.attribute_friendly_name_document_number,
        ValueType.STRING
    ),
    ADMINISTRATIVE_NUMBER("administrative_number", Res.string.attribute_friendly_name_administrative_number,
        ValueType.STRING
    ),
    ISSUING_COUNTRY("issuing_country", Res.string.attribute_friendly_name_issuing_country,
        ValueType.STRING
    ),
    ISSUING_JURISDICTION("issuing_jurisdiction", Res.string.attribute_friendly_name_distinguishing_sign,
        ValueType.STRING
    ),
    DRIVING_PRIVILEGES("driving_privileges", Res.string.attribute_friendly_name_driving_privileges,
        ValueType.DRIVING_PRIVILEGES
    );

    companion object {
        private val byValue = entries.associateBy { it.value }

        fun fromValue(value: String): DocumentAttributes? = byValue[value]

        fun getValuesFromDisplayNames(displayNames: Set<StringResource>): Set<String> {
            return displayNames.mapNotNull { displayName ->
                entries.find { it.displayName == displayName }?.value
            }.toSet()
        }
    }
}

fun itemsToRequestDocument(
    docType: String,
    namespace: String,
    entries: Set<StringResource>
): RequestDocument {
    return RequestDocument(
        docType = docType,
        itemsToRequest = mapOf(
            namespace to DocumentAttributes.getValuesFromDisplayNames(entries).associateWith { false }
        )
    )
}

fun getIdentityRequestDocument(): RequestDocument {
    return RequestDocument(
        docType = IDENTITY,
        itemsToRequest = mapOf(
            MDL_NAMESPACE to mapOf(
                "portrait" to false,
                "birth_date" to true,
                "given_name" to true,
                "family_name" to true
            )
        )
    )
}

fun getMdlRequestDocument(): RequestDocument {
    return RequestDocument(
        docType = MDL_DOCTYPE,
        itemsToRequest = mapOf(
            MDL_NAMESPACE to mapOf(
                "portrait" to false,
                "birth_date" to true,
                "given_name" to true,
                "issue_date" to true,
                "birth_place" to true,
                "expiry_date" to true,
                "family_name" to true,
                "document_number" to true,
                "issuing_authority" to true,
                "driving_privileges" to true,
                "signature_usual_mark" to true
            )
        )
    )
}

fun getAgeVerificationRequestDocument(age: Int): RequestDocument {
    return RequestDocument(
        docType = AGE_VERIFICATION,
        itemsToRequest = mapOf(
            MDL_NAMESPACE to mapOf(
                "portrait" to false,
                "age_over_$age" to false
            )
        )
    )
}

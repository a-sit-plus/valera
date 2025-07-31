@file:Suppress("DEPRECATION")

package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.ehic.EhicScheme.Attributes.AUTHENTIC_SOURCE_ID
import at.asitplus.wallet.ehic.EhicScheme.Attributes.AUTHENTIC_SOURCE_NAME
import at.asitplus.wallet.ehic.EhicScheme.Attributes.DATE_OF_EXPIRY
import at.asitplus.wallet.ehic.EhicScheme.Attributes.DATE_OF_ISSUANCE
import at.asitplus.wallet.ehic.EhicScheme.Attributes.DOCUMENT_NUMBER
import at.asitplus.wallet.ehic.EhicScheme.Attributes.ENDING_DATE
import at.asitplus.wallet.ehic.EhicScheme.Attributes.EXPIRY_DATE
import at.asitplus.wallet.ehic.EhicScheme.Attributes.ISSUANCE_DATE
import at.asitplus.wallet.ehic.EhicScheme.Attributes.ISSUING_AUTHORITY_ID
import at.asitplus.wallet.ehic.EhicScheme.Attributes.ISSUING_AUTHORITY_NAME
import at.asitplus.wallet.ehic.EhicScheme.Attributes.ISSUING_COUNTRY
import at.asitplus.wallet.ehic.EhicScheme.Attributes.IssuingAuthority.ID
import at.asitplus.wallet.ehic.EhicScheme.Attributes.IssuingAuthority.NAME
import at.asitplus.wallet.ehic.EhicScheme.Attributes.PERSONAL_ADMINISTRATIVE_NUMBER
import at.asitplus.wallet.ehic.EhicScheme.Attributes.PREFIX_AUTHENTIC_SOURCE
import at.asitplus.wallet.ehic.EhicScheme.Attributes.PREFIX_ISSUING_AUTHORITY
import at.asitplus.wallet.ehic.EhicScheme.Attributes.SOCIAL_SECURITY_NUMBER
import at.asitplus.wallet.ehic.EhicScheme.Attributes.STARTING_DATE
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import data.Attribute
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

sealed class EhicCredentialAdapter : CredentialAdapter() {
    @Suppress("DEPRECATION")
    override fun getAttribute(path: NormalizedJsonPath) =
        path.segments.firstOrNull()?.let { first ->
            with(EhicScheme.Attributes) {
                when (first) {
                    is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                        ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
                        SOCIAL_SECURITY_NUMBER -> Attribute.fromValue(socialSecurityNumber)
                        PERSONAL_ADMINISTRATIVE_NUMBER -> Attribute.fromValue(personalAdministrativeNumber)
                        PREFIX_ISSUING_AUTHORITY -> when (val second =
                            path.segments.drop(1).firstOrNull()) {
                            is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                                ID -> Attribute.fromValue(issuingAuthorityId)
                                NAME -> Attribute.fromValue(issuingAuthorityName)
                                else -> null
                            }

                            else -> Attribute.fromValue(issuingAuthority)
                        }
                        ISSUING_AUTHORITY_ID -> Attribute.fromValue(issuingAuthorityId)
                        ISSUING_AUTHORITY_NAME -> Attribute.fromValue(issuingAuthorityName)
                        PREFIX_AUTHENTIC_SOURCE -> when (val second =
                            path.segments.drop(1).firstOrNull()) {
                            is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                                ID -> Attribute.fromValue(authenticSourceId)
                                NAME -> Attribute.fromValue(authenticSourceName)
                                else -> null
                            }

                            else -> Attribute.fromValue(authenticSource)
                        }
                        AUTHENTIC_SOURCE_ID -> Attribute.fromValue(authenticSourceId)
                        AUTHENTIC_SOURCE_NAME -> Attribute.fromValue(authenticSourceName)
                        DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
                        ISSUANCE_DATE -> Attribute.fromValue(issuanceDate)
                        DATE_OF_ISSUANCE -> Attribute.fromValue(dateOfIssuance)
                        EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                        DATE_OF_EXPIRY -> Attribute.fromValue(dateOfExpiry)
                        STARTING_DATE -> Attribute.fromValue(startingDate)
                        ENDING_DATE -> Attribute.fromValue(endingDate)
                        else -> null
                    }

                    else -> null
                }
            }
        }

    abstract val issuingCountry: String?
    abstract val socialSecurityNumber: String?
    abstract val personalAdministrativeNumber: String?
    abstract val issuingAuthority: String?
    abstract val issuingAuthorityId: String?
    abstract val issuingAuthorityName: String?
    abstract val authenticSource: String?
    abstract val authenticSourceId: String?
    abstract val authenticSourceName: String?
    abstract val documentNumber: String?
    abstract val issuanceDate: LocalDate?
    abstract val dateOfIssuance: LocalDate?
    abstract val expiryDate: LocalDate?
    abstract val dateOfExpiry: LocalDate?
    abstract val startingDate: LocalDate?
    abstract val endingDate: LocalDate?

    companion object {
        fun createFromStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry): EhicCredentialAdapter {
            if (storeEntry.scheme !is EhicScheme) {
                throw IllegalArgumentException("credential")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.SdJwt -> storeEntry.toComplexJson()
                    ?.let { EhicComplexCredentialSdJwtAdapter(it) } ?: EhicCredentialSdJwtAdapter(
                    storeEntry.toAttributeMap()
                )

                else -> TODO("Operation not yet supported")
            }
        }
    }
}

private class EhicCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>,
) : EhicCredentialAdapter() {
    override val scheme: ConstantIndex.CredentialScheme
        get() = EhicScheme

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val socialSecurityNumber: String?
        get() = attributes[SOCIAL_SECURITY_NUMBER]?.contentOrNull

    override val personalAdministrativeNumber: String?
        get() = attributes[PERSONAL_ADMINISTRATIVE_NUMBER]?.contentOrNull

    override val documentNumber: String?
        get() = attributes[DOCUMENT_NUMBER]?.contentOrNull

    override val issuingAuthority: String?
        get() = attributes[PREFIX_ISSUING_AUTHORITY]?.contentOrNull

    override val issuingAuthorityId: String?
        get() = attributes[ISSUING_AUTHORITY_ID]?.contentOrNull

    override val issuingAuthorityName: String?
        get() = attributes[ISSUING_AUTHORITY_NAME]?.contentOrNull

    override val authenticSource: String?
        get() = attributes[PREFIX_AUTHENTIC_SOURCE]?.contentOrNull

    override val authenticSourceId: String?
        get() = attributes[AUTHENTIC_SOURCE_ID]?.contentOrNull

    override val authenticSourceName: String?
        get() = attributes[AUTHENTIC_SOURCE_NAME]?.contentOrNull

    override val issuanceDate: LocalDate?
        get() = attributes[ISSUANCE_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val dateOfIssuance: LocalDate?
        get() = attributes[DATE_OF_ISSUANCE]?.contentOrNull?.toLocalDateOrNull()

    override val expiryDate: LocalDate?
        get() = attributes[EXPIRY_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val dateOfExpiry: LocalDate?
        get() = attributes[DATE_OF_EXPIRY]?.contentOrNull?.toLocalDateOrNull()

    override val startingDate: LocalDate?
        get() = attributes[STARTING_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val endingDate: LocalDate?
        get() = attributes[ENDING_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val issuingCountry: String?
        get() = attributes[ISSUING_COUNTRY]?.contentOrNull
}


private class EhicComplexCredentialSdJwtAdapter(
    private val attributes: JsonObject,
) : EhicCredentialAdapter() {
    override val scheme: ConstantIndex.CredentialScheme
        get() = EhicScheme

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val socialSecurityNumber: String?
        get() = (attributes[SOCIAL_SECURITY_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val personalAdministrativeNumber: String?
        get() = (attributes[PERSONAL_ADMINISTRATIVE_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val documentNumber: String?
        get() = (attributes[DOCUMENT_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val issuingAuthority: String?
        get() = (attributes[PREFIX_ISSUING_AUTHORITY] as? JsonPrimitive?)?.contentOrNull

    override val issuingAuthorityId: String?
        get() = (attributes[PREFIX_ISSUING_AUTHORITY] as? JsonObject?)?.let {
            (it[ID] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[ISSUING_AUTHORITY_ID] as? JsonPrimitive?)?.contentOrNull

    override val issuingAuthorityName: String?
        get() = (attributes[PREFIX_ISSUING_AUTHORITY] as? JsonObject?)?.let {
            (it[NAME] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[ISSUING_AUTHORITY_NAME] as? JsonPrimitive?)?.contentOrNull

    override val authenticSource: String?
        get() = (attributes[PREFIX_AUTHENTIC_SOURCE] as? JsonPrimitive?)?.contentOrNull

    override val authenticSourceId: String?
        get() = (attributes[PREFIX_AUTHENTIC_SOURCE] as? JsonObject?)?.let {
            (it[ID] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[AUTHENTIC_SOURCE_ID] as? JsonPrimitive?)?.contentOrNull

    override val authenticSourceName: String?
        get() = (attributes[PREFIX_AUTHENTIC_SOURCE] as? JsonObject?)?.let {
            (it[NAME] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[AUTHENTIC_SOURCE_NAME] as? JsonPrimitive?)?.contentOrNull

    override val issuanceDate: LocalDate?
        get() = (attributes[ISSUANCE_DATE] as? JsonPrimitive?)?.contentOrNull?.toLocalDateOrNull()

    override val dateOfIssuance: LocalDate?
        get() = (attributes[DATE_OF_ISSUANCE] as? JsonPrimitive?)?.contentOrNull?.toLocalDateOrNull()

    override val expiryDate: LocalDate?
        get() = (attributes[EXPIRY_DATE] as? JsonPrimitive?)?.contentOrNull?.toLocalDateOrNull()

    override val dateOfExpiry: LocalDate?
        get() = (attributes[DATE_OF_EXPIRY] as? JsonPrimitive?)?.contentOrNull?.toLocalDateOrNull()
    
    override val startingDate: LocalDate?
        get() = (attributes[STARTING_DATE] as? JsonPrimitive?)?.contentOrNull?.toLocalDateOrNull()

    override val endingDate: LocalDate?
        get() = (attributes[ENDING_DATE] as? JsonPrimitive?)?.contentOrNull?.toLocalDateOrNull()

    override val issuingCountry: String?
        get() = (attributes[ISSUING_COUNTRY] as? JsonPrimitive?)?.contentOrNull
}

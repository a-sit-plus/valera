package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.taxid.TaxIdScheme
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.ADMINISTRATIVE_NUMBER
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.AFFILIATION_COUNTRY
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.BIRTH_DATE
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.CHURCH_TAX_ID
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.DOCUMENT_NUMBER
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.EXPIRY_DATE
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.IBAN
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.ISSUANCE_DATE
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.ISSUING_AUTHORITY
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.ISSUING_COUNTRY
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.ISSUING_JURISDICTION
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.PID_ID
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.REGISTERED_FAMILY_NAME
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.REGISTERED_GIVEN_NAME
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.RESIDENT_ADDRESS
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.TAX_NUMBER
import at.asitplus.wallet.taxid.TaxIdScheme.Attributes.VERIFICATION_STATUS
import data.Attribute
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

sealed class TaxIdCredentialAdapter : CredentialAdapter() {

    override fun getAttribute(path: NormalizedJsonPath) = path.segments.firstOrNull()?.let { first ->
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                TAX_NUMBER -> Attribute.fromValue(taxNumber)
                AFFILIATION_COUNTRY -> Attribute.fromValue(affiliationCountry)
                REGISTERED_FAMILY_NAME -> Attribute.fromValue(registeredFamilyName)
                REGISTERED_GIVEN_NAME -> Attribute.fromValue(registeredGivenName)
                RESIDENT_ADDRESS -> Attribute.fromValue(residentAddress)
                BIRTH_DATE -> Attribute.fromValue(birthDate)
                CHURCH_TAX_ID -> Attribute.fromValue(churchTaxId)
                IBAN -> Attribute.fromValue(iban)
                PID_ID -> Attribute.fromValue(pidId)
                ISSUANCE_DATE -> Attribute.fromValue(issuanceDate)
                VERIFICATION_STATUS -> Attribute.fromValue(verificationStatus)
                EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                ISSUING_AUTHORITY -> Attribute.fromValue(issuingAuthority)
                DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
                ADMINISTRATIVE_NUMBER -> Attribute.fromValue(administrativeNumber)
                ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
                ISSUING_JURISDICTION -> Attribute.fromValue(issuingJurisdiction)

                else -> null
            }

            else -> null
        }
    }

    abstract val taxNumber: String?
    abstract val affiliationCountry: String?
    abstract val registeredFamilyName: String?
    abstract val registeredGivenName: String?
    abstract val residentAddress: String?
    abstract val birthDate: LocalDate?
    abstract val churchTaxId: String?
    abstract val iban: String?
    abstract val pidId: String?
    abstract val issuanceDate: Instant?
    abstract val verificationStatus: String?
    abstract val expiryDate: Instant?
    abstract val issuingAuthority: String?
    abstract val documentNumber: String?
    abstract val administrativeNumber: String?
    abstract val issuingCountry: String?
    abstract val issuingJurisdiction: String?

    companion object {
        @Suppress("DEPRECATION")
        fun createFromStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry): TaxIdCredentialAdapter {
            val scheme = storeEntry.scheme
                ?: throw IllegalArgumentException("credential scheme null: $storeEntry")
            if (scheme !is at.asitplus.wallet.taxid.TaxIdScheme && scheme !is TaxIdScheme) {
                throw IllegalArgumentException("credential scheme unknown: $scheme")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> TODO("Operation not yet supported")
                is SubjectCredentialStore.StoreEntry.SdJwt -> storeEntry.toComplexJson()
                    ?.let { TaxIdComplexCredentialSdJwtAdapter(it, scheme) }
                    ?: TaxIdCredentialSdJwtAdapter(storeEntry.toAttributeMap(), scheme)

                is SubjectCredentialStore.StoreEntry.Iso -> TaxIdIsoMdocAdapter(storeEntry.toNamespaceAttributeMap(),
                    scheme
                )
            }
        }
    }
}

private class TaxIdCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>,
    override val scheme: ConstantIndex.CredentialScheme
) : TaxIdCredentialAdapter() {
    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val taxNumber: String?
        get() = attributes[TAX_NUMBER]?.contentOrNull

    override val affiliationCountry: String?
        get() = attributes[AFFILIATION_COUNTRY]?.contentOrNull

    override val registeredFamilyName: String?
        get() = attributes[REGISTERED_FAMILY_NAME]?.contentOrNull

    override val registeredGivenName: String?
        get() = attributes[REGISTERED_GIVEN_NAME]?.contentOrNull

    override val residentAddress: String?
        get() = attributes[RESIDENT_ADDRESS]?.contentOrNull

    override val birthDate: LocalDate?
        get() = attributes[BIRTH_DATE]?.toLocalDateOrNull()

    override val churchTaxId: String?
        get() = attributes[CHURCH_TAX_ID]?.contentOrNull

    override val iban: String?
        get() = attributes[IBAN]?.contentOrNull

    override val pidId: String?
        get() = attributes[PID_ID]?.contentOrNull

    override val issuanceDate: Instant?
        get() = attributes[ISSUANCE_DATE]?.contentOrNull.toInstantOrNull()

    override val verificationStatus: String?
        get() = attributes[VERIFICATION_STATUS]?.contentOrNull

    override val expiryDate: Instant?
        get() = attributes[EXPIRY_DATE]?.contentOrNull.toInstantOrNull()

    override val issuingAuthority: String?
        get() = attributes[ISSUING_AUTHORITY]?.contentOrNull

    override val documentNumber: String?
        get() = attributes[DOCUMENT_NUMBER]?.contentOrNull

    override val administrativeNumber: String?
        get() = attributes[ADMINISTRATIVE_NUMBER]?.contentOrNull

    override val issuingCountry: String?
        get() = attributes[ISSUING_COUNTRY]?.contentOrNull

    override val issuingJurisdiction: String?
        get() = attributes[ISSUING_JURISDICTION]?.contentOrNull
}

private class TaxIdComplexCredentialSdJwtAdapter(
    private val attributes: JsonObject,
    override val scheme: ConstantIndex.CredentialScheme
) : TaxIdCredentialAdapter() {
    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val taxNumber: String?
        get() = (attributes[TAX_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val affiliationCountry: String?
        get() = (attributes[AFFILIATION_COUNTRY] as? JsonPrimitive?)?.contentOrNull

    override val registeredFamilyName: String?
        get() = (attributes[REGISTERED_FAMILY_NAME] as? JsonPrimitive?)?.contentOrNull

    override val registeredGivenName: String?
        get() = (attributes[REGISTERED_GIVEN_NAME] as? JsonPrimitive?)?.contentOrNull

    override val residentAddress: String?
        get() = (attributes[RESIDENT_ADDRESS] as? JsonPrimitive?)?.contentOrNull

    override val birthDate: LocalDate?
        get() = (attributes[BIRTH_DATE] as? JsonPrimitive?)?.contentOrNull?.toLocalDateOrNull()

    override val churchTaxId: String?
        get() = (attributes[CHURCH_TAX_ID] as? JsonPrimitive?)?.contentOrNull

    override val iban: String?
        get() = (attributes[IBAN] as? JsonPrimitive?)?.contentOrNull

    override val pidId: String?
        get() = (attributes[PID_ID] as? JsonPrimitive?)?.contentOrNull

    override val issuanceDate: Instant?
        get() = (attributes[ISSUANCE_DATE] as? JsonPrimitive?)?.contentOrNull.toInstantOrNull()

    override val verificationStatus: String?
        get() = (attributes[VERIFICATION_STATUS] as? JsonPrimitive?)?.contentOrNull

    override val expiryDate: Instant?
        get() = (attributes[EXPIRY_DATE] as? JsonPrimitive?)?.contentOrNull.toInstantOrNull()

    override val issuingAuthority: String?
        get() = (attributes[ISSUING_AUTHORITY] as? JsonPrimitive?)?.contentOrNull

    override val documentNumber: String?
        get() = (attributes[DOCUMENT_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val administrativeNumber: String?
        get() = (attributes[ADMINISTRATIVE_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val issuingCountry: String?
        get() = (attributes[ISSUING_COUNTRY] as? JsonPrimitive?)?.contentOrNull

    override val issuingJurisdiction: String?
        get() = (attributes[ISSUING_JURISDICTION] as? JsonPrimitive?)?.contentOrNull
}

private class TaxIdIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
    override val scheme: ConstantIndex.CredentialScheme
) : TaxIdCredentialAdapter() {
    private val namespace = namespaces?.get(scheme.isoNamespace)

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.ISO_MDOC

    override val taxNumber: String?
        get() = namespace?.get(TAX_NUMBER) as String?

    override val affiliationCountry: String?
        get() = namespace?.get(AFFILIATION_COUNTRY) as String?

    override val registeredFamilyName: String?
        get() = namespace?.get(REGISTERED_FAMILY_NAME) as String?

    override val registeredGivenName: String?
        get() = namespace?.get(REGISTERED_GIVEN_NAME) as String?

    override val residentAddress: String?
        get() = namespace?.get(RESIDENT_ADDRESS) as String?

    override val birthDate: LocalDate?
        get() = namespace?.get(BIRTH_DATE)?.toLocalDateOrNull()

    override val churchTaxId: String?
        get() = namespace?.get(CHURCH_TAX_ID) as String?

    override val iban: String?
        get() = namespace?.get(IBAN) as String?

    override val pidId: String?
        get() = namespace?.get(PID_ID) as String?

    override val issuanceDate: Instant?
        get() = namespace?.get(ISSUANCE_DATE).toInstantOrNull()

    override val verificationStatus: String?
        get() = namespace?.get(VERIFICATION_STATUS) as String?

    override val expiryDate: Instant?
        get() = namespace?.get(EXPIRY_DATE).toInstantOrNull()

    override val issuingAuthority: String?
        get() = namespace?.get(ISSUING_AUTHORITY) as String?

    override val documentNumber: String?
        get() = namespace?.get(DOCUMENT_NUMBER) as String?

    override val administrativeNumber: String?
        get() = namespace?.get(ADMINISTRATIVE_NUMBER) as String?

    override val issuingCountry: String?
        get() = namespace?.get(ISSUING_COUNTRY) as String?

    override val issuingJurisdiction: String?
        get() = namespace?.get(ISSUING_JURISDICTION) as String?
}

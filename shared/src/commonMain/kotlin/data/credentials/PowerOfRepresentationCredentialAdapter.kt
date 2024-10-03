package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.por.PowerOfRepresentationDataElements
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.ADMINISTRATIVE_NUMBER
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.DOCUMENT_NUMBER
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.EFFECTIVE_FROM_DATE
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.EFFECTIVE_UNTIL_DATE
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.EXPIRY_DATE
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.E_SERVICE
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.FULL_POWERS
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.ISSUANCE_DATE
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.ISSUING_AUTHORITY
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.ISSUING_COUNTRY
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.ISSUING_JURISDICTION
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.LEGAL_NAME
import at.asitplus.wallet.por.PowerOfRepresentationDataElements.LEGAL_PERSON_IDENTIFIER
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import data.Attribute
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

sealed class PowerOfRepresentationCredentialAdapter : CredentialAdapter() {

    override fun getAttribute(path: NormalizedJsonPath) = path.segments.firstOrNull()?.let { first ->
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                LEGAL_PERSON_IDENTIFIER -> Attribute.fromValue(legalPersonIdentifier)
                LEGAL_NAME -> Attribute.fromValue(legalName)
                FULL_POWERS -> Attribute.fromValue(fullPowers)
                E_SERVICE -> Attribute.fromValue(eService)
                EFFECTIVE_FROM_DATE -> Attribute.fromValue(effectiveFromDate)
                EFFECTIVE_UNTIL_DATE -> Attribute.fromValue(effectiveUntilDate)
                ADMINISTRATIVE_NUMBER -> Attribute.fromValue(administrativeNumber)
                ISSUANCE_DATE -> Attribute.fromValue(issuanceDate)
                EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                ISSUING_AUTHORITY -> Attribute.fromValue(issuingAuthority)
                DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
                ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
                ISSUING_JURISDICTION -> Attribute.fromValue(issuingJurisdiction)
                else -> null
            }

            else -> null
        }
    }

    abstract val legalPersonIdentifier: String?
    abstract val legalName: String?
    abstract val fullPowers: Boolean?
    abstract val eService: String?
    abstract val effectiveFromDate: Instant?
    abstract val effectiveUntilDate: Instant?
    abstract val administrativeNumber: String?
    abstract val issuanceDate: Instant?
    abstract val expiryDate: Instant?
    abstract val issuingAuthority: String?
    abstract val documentNumber: String?
    abstract val issuingCountry: String?
    abstract val issuingJurisdiction: String?

    companion object {
        fun createFromStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry): PowerOfRepresentationCredentialAdapter {
            if (storeEntry.scheme !is PowerOfRepresentationScheme) {
                throw IllegalArgumentException("credential")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    TODO("Operation not yet supported")
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    PowerOfRepresentationCredentialSdJwtAdapter(
                        storeEntry.toAttributeMap(),
                    )
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    PowerOfRepresentationCredentialIsoMdocAdapter(
                        storeEntry.toNamespaceAttributeMap(),
                    )
                }
            }
        }
    }
}

private class PowerOfRepresentationCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>,
) : PowerOfRepresentationCredentialAdapter() {

    override val documentNumber: String?
        get() = attributes[PowerOfRepresentationDataElements.DOCUMENT_NUMBER]?.contentOrNull

    override val issuingAuthority: String?
        get() = attributes[PowerOfRepresentationDataElements.ISSUING_AUTHORITY]?.contentOrNull

    override val legalPersonIdentifier: String?
        get() = attributes[PowerOfRepresentationDataElements.LEGAL_PERSON_IDENTIFIER]?.contentOrNull

    override val legalName: String?
        get() = attributes[PowerOfRepresentationDataElements.LEGAL_NAME]?.contentOrNull

    override val fullPowers: Boolean?
        get() = attributes[PowerOfRepresentationDataElements.FULL_POWERS]?.booleanOrNull

    override val eService: String?
        get() = attributes[PowerOfRepresentationDataElements.E_SERVICE]?.contentOrNull

    override val effectiveFromDate: Instant?
        get() = attributes[PowerOfRepresentationDataElements.EFFECTIVE_FROM_DATE]?.contentOrNull?.toInstantOrNull()

    override val effectiveUntilDate: Instant?
        get() = attributes[PowerOfRepresentationDataElements.EFFECTIVE_UNTIL_DATE]?.contentOrNull?.toInstantOrNull()

    override val administrativeNumber: String?
        get() = attributes[PowerOfRepresentationDataElements.ADMINISTRATIVE_NUMBER]?.contentOrNull

    override val issuanceDate: Instant?
        get() = attributes[PowerOfRepresentationDataElements.ISSUANCE_DATE]?.contentOrNull?.toInstantOrNull()

    override val expiryDate: Instant?
        get() = attributes[PowerOfRepresentationDataElements.EXPIRY_DATE]?.contentOrNull?.toInstantOrNull()

    override val issuingCountry: String?
        get() = attributes[PowerOfRepresentationDataElements.ISSUING_COUNTRY]?.contentOrNull

    override val issuingJurisdiction: String?
        get() = attributes[PowerOfRepresentationDataElements.ISSUING_JURISDICTION]?.contentOrNull
}

private class PowerOfRepresentationCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
) : PowerOfRepresentationCredentialAdapter() {
    private val mobileDrivingLicenceNamespace =
        namespaces?.get(PowerOfRepresentationScheme.toString())
            ?: throw IllegalArgumentException("namespaces") // contains required attributes


    override val documentNumber: String?
        get() = mobileDrivingLicenceNamespace[DOCUMENT_NUMBER] as String?

    override val issuingAuthority: String?
        get() = mobileDrivingLicenceNamespace[ISSUING_AUTHORITY] as String?

    override val legalPersonIdentifier: String?
        get() = mobileDrivingLicenceNamespace[LEGAL_PERSON_IDENTIFIER] as String?

    override val legalName: String?
        get() = mobileDrivingLicenceNamespace[LEGAL_NAME] as String?

    override val fullPowers: Boolean?
        get() = mobileDrivingLicenceNamespace[FULL_POWERS] as Boolean?

    override val eService: String?
        get() = mobileDrivingLicenceNamespace[E_SERVICE] as String?

    override val effectiveFromDate: Instant?
        get() = mobileDrivingLicenceNamespace[EFFECTIVE_FROM_DATE].toInstantOrNull()

    override val effectiveUntilDate: Instant?
        get() = mobileDrivingLicenceNamespace[EFFECTIVE_UNTIL_DATE].toInstantOrNull()

    override val administrativeNumber: String?
        get() = mobileDrivingLicenceNamespace[ADMINISTRATIVE_NUMBER] as String?

    override val issuanceDate: Instant?
        get() = mobileDrivingLicenceNamespace[ISSUANCE_DATE].toInstantOrNull()

    override val expiryDate: Instant?
        get() = mobileDrivingLicenceNamespace[EXPIRY_DATE].toInstantOrNull()

    override val issuingCountry: String?
        get() = mobileDrivingLicenceNamespace[ISSUING_COUNTRY] as String?

    override val issuingJurisdiction: String?
        get() = mobileDrivingLicenceNamespace[ISSUING_JURISDICTION] as String?
}
package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catchingUnwrapped
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.memberName
import at.asitplus.wallet.app.common.minus
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.eupid.PlaceOfBirth
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.data.LocalDateOrInstant
import data.Attribute
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.io.encoding.Base64
import at.asitplus.wallet.eupid.EuPidDataElements as Attributes
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtDataElements as SdJwtAttributes
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtDataElements.Address as SdJwtAddress
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtDataElements.PlaceOfBirth as SdJwtPlaceOfBirth

sealed class EuPidCredentialAdapter(
    private val decodePortrait: (ByteArray) -> Result<ImageBitmap>,
) : CredentialAdapter() {

    override fun getAttribute(path: NormalizedJsonPath) =
        path.minus(EU_PID_DOCTYPE).let {
            it.memberName(0)?.let { claim ->
                mdocClaimToAttribute(EU_PID_DOCTYPE, claim) ?: sdJwtPathToAttribute(it)
            } ?: sdJwtPathToAttribute(it)
        }

    abstract val givenName: String?
    abstract val familyName: String?
    abstract val birthDate: LocalDate?
    abstract val portraitRaw: ByteArray?
    val portraitBitmap: ImageBitmap? by lazy {
        portraitRaw?.let(decodePortrait)?.getOrNull()
    }
    abstract val residentAddress: String?
    abstract val residentStreet: String?
    abstract val residentCity: String?
    abstract val residentPostalCode: String?
    abstract val residentHouseNumber: String?
    abstract val residentCountry: String?
    abstract val residentState: String?
    abstract val sex: String?
    abstract val nationality: String?
    abstract val nationalities: Collection<String>?
    abstract val familyNameBirth: String?
    abstract val givenNameBirth: String?
    abstract val placeOfBirth: PlaceOfBirth?
    abstract val issuanceDate: LocalDateOrInstant?
    abstract val expiryDate: LocalDateOrInstant?
    abstract val issuingAuthority: String?
    abstract val documentNumber: String?
    abstract val issuingCountry: String?
    abstract val issuingJurisdiction: String?
    abstract val personalAdministrativeNumber: String?
    abstract val emailAddress: String?
    abstract val mobilePhoneNumber: String?
    abstract val trustAnchor: String?
    abstract val locationStatus: String?

    companion object {
        fun createFromStoreEntry(
            storeEntry: SubjectCredentialStore.StoreEntry,
            decodePortrait: (ByteArray) -> Result<ImageBitmap>,
        ): EuPidCredentialAdapter {
            if (!storeEntry.scheme.isEuPid) {
                throw IllegalArgumentException("credential: ${storeEntry.scheme}")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> TODO("Operation not yet supported")

                is SubjectCredentialStore.StoreEntry.SdJwt -> EuPidCredentialSdJwtAdapter(
                    attributes = storeEntry.toAttributeMap(),
                    complexJson = storeEntry.toComplexJson(),
                    decodePortrait = decodePortrait,
                    scheme = storeEntry.scheme
                )

                is SubjectCredentialStore.StoreEntry.Iso -> EuPidCredentialIsoMdocAdapter(
                    namespaces = storeEntry.toNamespaceAttributeMap(), decodePortrait = decodePortrait,
                    scheme = storeEntry.scheme
                )
            }
        }
    }

    private fun mdocClaimToAttribute(namespace: String, claimName: String): Attribute? =
        if (namespace != EU_PID_DOCTYPE) null else with(Attributes) {
            when (claimName) {
                GIVEN_NAME -> Attribute.fromValue(givenName)
                FAMILY_NAME -> Attribute.fromValue(familyName)
                BIRTH_DATE -> Attribute.fromValue(birthDate)
                RESIDENT_ADDRESS -> null
                RESIDENT_STREET -> Attribute.fromValue(residentStreet)
                RESIDENT_CITY -> Attribute.fromValue(residentCity)
                RESIDENT_POSTAL_CODE -> Attribute.fromValue(residentPostalCode)
                RESIDENT_HOUSE_NUMBER -> Attribute.fromValue(residentHouseNumber)
                RESIDENT_COUNTRY -> Attribute.fromValue(residentCountry)
                RESIDENT_STATE -> Attribute.fromValue(residentState)
                SEX -> Attribute.fromValue(sex)
                NATIONALITY -> Attribute.fromValue(nationalities)
                FAMILY_NAME_BIRTH -> Attribute.fromValue(familyNameBirth)
                GIVEN_NAME_BIRTH -> Attribute.fromValue(givenNameBirth)
                PLACE_OF_BIRTH -> Attribute.fromValue(placeOfBirth)
                ISSUANCE_DATE -> Attribute.fromValue(issuanceDate)
                EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                ISSUING_AUTHORITY -> Attribute.fromValue(issuingAuthority)
                DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
                ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
                ISSUING_JURISDICTION -> Attribute.fromValue(issuingJurisdiction)
                PERSONAL_ADMINISTRATIVE_NUMBER -> Attribute.fromValue(personalAdministrativeNumber)
                PORTRAIT -> Attribute.fromValue(portraitBitmap)
                EMAIL_ADDRESS -> Attribute.fromValue(emailAddress)
                MOBILE_PHONE_NUMBER -> Attribute.fromValue(mobilePhoneNumber)
                TRUST_ANCHOR -> Attribute.fromValue(trustAnchor)
                LOCATION_STATUS -> Attribute.fromValue(locationStatus)
                else -> null
            }
        }

    private fun sdJwtPathToAttribute(path: NormalizedJsonPath): Attribute? = with(SdJwtAttributes) {
        when (path.memberName(0)) {
            GIVEN_NAME -> Attribute.fromValue(givenName)
            FAMILY_NAME -> Attribute.fromValue(familyName)
            BIRTH_DATE -> Attribute.fromValue(birthDate)
            PREFIX_ADDRESS -> sdJwtAddressAttribute(path)
            ADDRESS_FORMATTED -> Attribute.fromValue(residentAddress)
            ADDRESS_STREET -> Attribute.fromValue(residentStreet)
            ADDRESS_LOCALITY -> Attribute.fromValue(residentCity)
            ADDRESS_POSTAL_CODE -> Attribute.fromValue(residentPostalCode)
            ADDRESS_HOUSE_NUMBER -> Attribute.fromValue(residentHouseNumber)
            ADDRESS_COUNTRY -> Attribute.fromValue(residentCountry)
            ADDRESS_REGION -> Attribute.fromValue(residentState)
            SEX -> Attribute.fromValue(sex)
            NATIONALITIES -> Attribute.fromValue(nationalities)
            FAMILY_NAME_BIRTH -> Attribute.fromValue(familyNameBirth)
            GIVEN_NAME_BIRTH -> Attribute.fromValue(givenNameBirth)
            PREFIX_PLACE_OF_BIRTH -> sdJwtPlaceOfBirthAttribute(path)
            PLACE_OF_BIRTH_COUNTRY -> Attribute.fromValue(placeOfBirth?.country)
            PLACE_OF_BIRTH_REGION -> Attribute.fromValue(placeOfBirth?.region)
            PLACE_OF_BIRTH_LOCALITY -> Attribute.fromValue(placeOfBirth?.locality)
            ISSUANCE_DATE -> Attribute.fromValue(issuanceDate)
            EXPIRY_DATE -> Attribute.fromValue(expiryDate)
            ISSUING_AUTHORITY -> Attribute.fromValue(issuingAuthority)
            DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
            ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
            ISSUING_JURISDICTION -> Attribute.fromValue(issuingJurisdiction)
            PERSONAL_ADMINISTRATIVE_NUMBER -> Attribute.fromValue(personalAdministrativeNumber)
            PORTRAIT -> Attribute.fromValue(portraitBitmap)
            EMAIL -> Attribute.fromValue(emailAddress)
            PHONE_NUMBER -> Attribute.fromValue(mobilePhoneNumber)
            TRUST_ANCHOR -> Attribute.fromValue(trustAnchor)
            else -> null
        }
    }

    private fun sdJwtPlaceOfBirthAttribute(path: NormalizedJsonPath): Attribute? =
        when (path.memberName(1)) {
            SdJwtPlaceOfBirth.COUNTRY -> Attribute.fromValue(placeOfBirth?.country)
            SdJwtPlaceOfBirth.REGION -> Attribute.fromValue(placeOfBirth?.region)
            SdJwtPlaceOfBirth.LOCALITY -> Attribute.fromValue(placeOfBirth?.locality)
            else -> null
        }

    private fun sdJwtAddressAttribute(path: NormalizedJsonPath): Attribute? =
        when (path.memberName(1)) {
            SdJwtAddress.FORMATTED -> Attribute.fromValue(residentAddress)
            SdJwtAddress.STREET -> Attribute.fromValue(residentStreet)
            SdJwtAddress.LOCALITY -> Attribute.fromValue(residentCity)
            SdJwtAddress.POSTAL_CODE -> Attribute.fromValue(residentPostalCode)
            SdJwtAddress.HOUSE_NUMBER -> Attribute.fromValue(residentHouseNumber)
            SdJwtAddress.COUNTRY -> Attribute.fromValue(residentCountry)
            SdJwtAddress.REGION -> Attribute.fromValue(residentState)
            else -> null
        }
}

/**
 * Implements getting attributes for new names (from [SdJwtAttributes]),
 * as well as for old names (from [Attributes.GIVEN_NAME]), to keep data for credentials loaded before migration
 */
private class EuPidCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>,
    private val complexJson: JsonObject?,
    decodePortrait: (ByteArray) -> Result<ImageBitmap>,
    override val scheme: CredentialScheme
) : EuPidCredentialAdapter(decodePortrait) {
    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val givenName: String?
        get() = attributes[SdJwtAttributes.GIVEN_NAME]?.contentOrNull
            ?: attributes[Attributes.GIVEN_NAME]?.contentOrNull

    override val familyName: String?
        get() = attributes[SdJwtAttributes.FAMILY_NAME]?.contentOrNull
            ?: attributes[Attributes.FAMILY_NAME]?.contentOrNull

    override val birthDate: LocalDate?
        get() = attributes[SdJwtAttributes.BIRTH_DATE]?.contentOrNull?.toLocalDateOrNull()
            ?: attributes[Attributes.BIRTH_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val portraitRaw: ByteArray?
        get() = attributes[SdJwtAttributes.PORTRAIT]?.contentOrNull?.decodeFromPortraitString()
            ?: attributes[Attributes.PORTRAIT]?.contentOrNull?.decodeFromPortraitString()

    override val residentAddress: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAddress.FORMATTED)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_FORMATTED]?.contentOrNull

    override val residentStreet: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAddress.STREET)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_STREET]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_STREET]?.contentOrNull

    override val residentCity: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAddress.LOCALITY)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_LOCALITY]?.contentOrNull

    override val residentPostalCode: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAddress.POSTAL_CODE)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_POSTAL_CODE]?.contentOrNull

    override val residentHouseNumber: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAddress.HOUSE_NUMBER)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_HOUSE_NUMBER]?.contentOrNull

    override val residentCountry: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAddress.COUNTRY)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_COUNTRY]?.contentOrNull

    override val residentState: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAddress.REGION)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_REGION]?.contentOrNull

    override val sex: String?
        get() = attributes[SdJwtAttributes.SEX]?.contentOrNull?.toIntOrNull()
            ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code.toUInt() }?.name }

    override val nationality: String?
        get() = complexJson?.get(SdJwtAttributes.NATIONALITIES)?.toCollectionOrNull()?.firstOrNull()

    override val nationalities: Collection<String>?
        get() = complexJson?.get(SdJwtAttributes.NATIONALITIES)?.toCollectionOrNull()
            ?: attributes[SdJwtAttributes.NATIONALITIES]?.let { listOfNotNull(it.contentOrNull) }?.ifEmpty { null }
            ?: listOfNotNull(nationality).ifEmpty { null }

    override val familyNameBirth: String?
        get() = attributes[SdJwtAttributes.FAMILY_NAME_BIRTH]?.contentOrNull

    override val givenNameBirth: String?
        get() = attributes[SdJwtAttributes.GIVEN_NAME_BIRTH]?.contentOrNull

    override val placeOfBirth: PlaceOfBirth?
        get() = attributes[SdJwtAttributes.PREFIX_PLACE_OF_BIRTH]?.let {
            catchingUnwrapped { joseCompliantSerializer.decodeFromJsonElement<PlaceOfBirth>(it) }.getOrNull()
        }

    override val issuanceDate: LocalDateOrInstant?
        get() = attributes[SdJwtAttributes.ISSUANCE_DATE]?.contentOrNull?.toLocalDateOrInstantOrNull()

    override val expiryDate: LocalDateOrInstant?
        get() = attributes[SdJwtAttributes.EXPIRY_DATE]?.contentOrNull?.toLocalDateOrInstantOrNull()

    override val issuingAuthority: String?
        get() = attributes[SdJwtAttributes.ISSUING_AUTHORITY]?.contentOrNull

    override val documentNumber: String?
        get() = attributes[SdJwtAttributes.DOCUMENT_NUMBER]?.contentOrNull

    override val issuingCountry: String?
        get() = attributes[SdJwtAttributes.ISSUING_COUNTRY]?.contentOrNull

    override val issuingJurisdiction: String?
        get() = attributes[SdJwtAttributes.ISSUING_JURISDICTION]?.contentOrNull

    override val personalAdministrativeNumber: String?
        get() = attributes[SdJwtAttributes.PERSONAL_ADMINISTRATIVE_NUMBER]?.contentOrNull

    override val emailAddress: String?
        get() = attributes[SdJwtAttributes.EMAIL]?.contentOrNull

    override val mobilePhoneNumber: String?
        get() = attributes[SdJwtAttributes.PHONE_NUMBER]?.contentOrNull

    override val trustAnchor: String?
        get() = attributes[SdJwtAttributes.TRUST_ANCHOR]?.contentOrNull

    override val locationStatus: String?
        get() = null // per rulebook

}

class EuPidCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
    decodePortrait: (ByteArray) -> Result<ImageBitmap>,
    override val scheme: CredentialScheme
) : EuPidCredentialAdapter(decodePortrait) {
    private val euPidNamespace = namespaces?.get(EU_PID_DOCTYPE)

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.ISO_MDOC

    override val givenName: String?
        get() = euPidNamespace?.get(Attributes.GIVEN_NAME) as? String?

    override val familyName: String?
        get() = euPidNamespace?.get(Attributes.FAMILY_NAME) as? String?

    override val birthDate: LocalDate?
        get() = euPidNamespace?.get(Attributes.BIRTH_DATE) as? LocalDate?
            ?: euPidNamespace?.get(Attributes.BIRTH_DATE)?.toString()?.toLocalDateOrNull()

    override val portraitRaw: ByteArray?
        get() = euPidNamespace?.get(Attributes.PORTRAIT)?.let {
            when (it) {
                is ByteArray -> it
                is String -> catchingUnwrapped { Base64.decode(it) }.getOrNull()
                else -> null
            }
        }

    override val residentAddress: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_ADDRESS) as? String?

    override val residentStreet: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_STREET) as? String?

    override val residentCity: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_CITY) as? String?

    override val residentPostalCode: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_POSTAL_CODE)?.toString()

    override val residentHouseNumber: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_HOUSE_NUMBER)?.toString()

    override val residentCountry: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_COUNTRY) as? String?

    override val residentState: String?
        get() = euPidNamespace?.get(Attributes.RESIDENT_STATE) as? String?

    override val sex: String?
        get() = (euPidNamespace?.get(Attributes.SEX) as? UInt)
            ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code }?.name }

    override val nationality: String?
        get() = euPidNamespace?.get(Attributes.NATIONALITY) as? String?

    override val nationalities: Collection<String>?
        get() = (euPidNamespace?.get(Attributes.NATIONALITY) as? Collection<*>?)?.map { it.toString() }
            ?: listOfNotNull(nationality).ifEmpty { null }

    override val familyNameBirth: String?
        get() = euPidNamespace?.get(Attributes.FAMILY_NAME_BIRTH) as? String?

    override val givenNameBirth: String?
        get() = euPidNamespace?.get(Attributes.GIVEN_NAME_BIRTH) as? String?

    override val placeOfBirth: PlaceOfBirth?
        get() = euPidNamespace?.get(Attributes.PLACE_OF_BIRTH) as? PlaceOfBirth?

    override val issuanceDate: LocalDateOrInstant?
        get() = euPidNamespace?.get(Attributes.ISSUANCE_DATE) as? LocalDateOrInstant?
            ?: euPidNamespace?.get(Attributes.ISSUANCE_DATE) as? LocalDateOrInstant.LocalDate?
            ?: euPidNamespace?.get(Attributes.ISSUANCE_DATE) as? LocalDateOrInstant.Instant?
            ?: euPidNamespace?.get(Attributes.ISSUANCE_DATE)?.toString()
                ?.toLocalDateOrInstantOrNull()

    override val expiryDate: LocalDateOrInstant?
        get() = euPidNamespace?.get(Attributes.EXPIRY_DATE) as? LocalDateOrInstant?
            ?: euPidNamespace?.get(Attributes.EXPIRY_DATE) as? LocalDateOrInstant.LocalDate?
            ?: euPidNamespace?.get(Attributes.EXPIRY_DATE) as? LocalDateOrInstant.Instant?
            ?: euPidNamespace?.get(Attributes.EXPIRY_DATE)?.toString()
                ?.toLocalDateOrInstantOrNull()

    override val issuingAuthority: String?
        get() = euPidNamespace?.get(Attributes.ISSUING_AUTHORITY) as? String?

    override val documentNumber: String?
        get() = euPidNamespace?.get(Attributes.DOCUMENT_NUMBER) as? String?

    override val issuingCountry: String?
        get() = euPidNamespace?.get(Attributes.ISSUING_COUNTRY) as? String?

    override val issuingJurisdiction: String?
        get() = euPidNamespace?.get(Attributes.ISSUING_JURISDICTION) as? String?

    override val personalAdministrativeNumber: String?
        get() = euPidNamespace?.get(Attributes.PERSONAL_ADMINISTRATIVE_NUMBER) as? String?

    override val emailAddress: String?
        get() = euPidNamespace?.get(Attributes.EMAIL_ADDRESS) as? String?

    override val mobilePhoneNumber: String?
        get() = euPidNamespace?.get(Attributes.MOBILE_PHONE_NUMBER) as? String?

    override val trustAnchor: String?
        get() = euPidNamespace?.get(Attributes.TRUST_ANCHOR) as? String?

    override val locationStatus: String?
        get() = euPidNamespace?.get(Attributes.LOCATION_STATUS) as? String?

}

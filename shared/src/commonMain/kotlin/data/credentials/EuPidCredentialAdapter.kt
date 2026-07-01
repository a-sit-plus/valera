@file:Suppress("DEPRECATION")

package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catchingUnwrapped
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.memberName
import at.asitplus.wallet.app.common.minus
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.eupid.EuPidDataElements as Attributes
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.eupid.PlaceOfBirth
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtDataElements as SdJwtAttributes
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.data.LocalDateOrInstant
import data.Attribute
import io.ktor.util.decodeBase64Bytes
import kotlinx.datetime.LocalDate
import kotlinx.datetime.serializers.LocalDateIso8601Serializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

sealed class EuPidCredentialAdapter(
    private val decodePortrait: (ByteArray) -> Result<ImageBitmap>,
) : CredentialAdapter() {

    override fun getAttribute(path: NormalizedJsonPath) =
        path.minus(EU_PID_DOCTYPE).let {
            it.memberName(0)?.let { claim ->
                EuPidCredentialMdocClaimDefinitionResolver().resolveOrNull(EU_PID_DOCTYPE, claim)
                    ?.toAttribute()
                    ?: EuPidCredentialSdJwtClaimDefinitionResolver().resolveOrNull(it)
                        ?.toAttribute()
            }
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
            val scheme = storeEntry.scheme!!
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> (storeEntry.vc.vc.credentialSubject).let {
                    joseCompliantSerializer.decodeFromJsonElement<EuPidCredentialVcPayload>(it).let {
                        EuPidCredentialVcAdapter(it, decodePortrait, scheme)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt ->
                    EuPidCredentialSdJwtAdapter(
                        storeEntry.toAttributeMap(),
                        storeEntry.toComplexJson(),
                        decodePortrait,
                        scheme
                    )

                is SubjectCredentialStore.StoreEntry.Iso ->
                    EuPidCredentialIsoMdocAdapter(storeEntry.toNamespaceAttributeMap(), decodePortrait, scheme)
            }
        }
    }

    private fun EuPidCredentialClaimDefinition.toAttribute() = when (this) {
        EuPidCredentialClaimDefinition.GIVEN_NAME -> Attribute.fromValue(givenName)
        EuPidCredentialClaimDefinition.FAMILY_NAME -> Attribute.fromValue(familyName)
        EuPidCredentialClaimDefinition.BIRTH_DATE -> Attribute.fromValue(birthDate)
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_CONTAINER -> null
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_FORMATTED -> Attribute.fromValue(residentAddress)
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_STREET -> Attribute.fromValue(residentStreet)
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_LOCALITY -> Attribute.fromValue(residentCity)
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_POSTAL_CODE -> Attribute.fromValue(residentPostalCode)
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_HOUSE_NUMBER -> Attribute.fromValue(residentHouseNumber)
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_COUNTRY -> Attribute.fromValue(residentCountry)
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_REGION -> Attribute.fromValue(residentState)
        EuPidCredentialClaimDefinition.SEX -> Attribute.fromValue(sex)
        EuPidCredentialClaimDefinition.NATIONALITIES -> Attribute.fromValue(nationalities)
        EuPidCredentialClaimDefinition.FAMILY_NAME_BIRTH -> Attribute.fromValue(familyNameBirth)
        EuPidCredentialClaimDefinition.GIVEN_NAME_BIRTH -> Attribute.fromValue(givenNameBirth)
        EuPidCredentialClaimDefinition.PLACE_OF_BIRTH -> Attribute.fromValue(placeOfBirth)
        EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_CONTAINER -> null
        EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_COUNTRY -> Attribute.fromValue(placeOfBirth?.country)
        EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_REGION -> Attribute.fromValue(placeOfBirth?.region)
        EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_LOCALITY -> Attribute.fromValue(placeOfBirth?.locality)
        EuPidCredentialClaimDefinition.ISSUANCE_DATE -> Attribute.fromValue(issuanceDate)
        EuPidCredentialClaimDefinition.EXPIRY_DATE -> Attribute.fromValue(expiryDate)
        EuPidCredentialClaimDefinition.ISSUING_AUTHORITY -> Attribute.fromValue(issuingAuthority)
        EuPidCredentialClaimDefinition.DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
        EuPidCredentialClaimDefinition.ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
        EuPidCredentialClaimDefinition.ISSUING_JURISDICTION -> Attribute.fromValue(issuingJurisdiction)
        EuPidCredentialClaimDefinition.PERSONAL_ADMINISTRATIVE_NUMBER -> Attribute.fromValue(
            personalAdministrativeNumber
        )
        EuPidCredentialClaimDefinition.PORTRAIT -> Attribute.fromValue(portraitBitmap)
        EuPidCredentialClaimDefinition.EMAIL_ADDRESS -> Attribute.fromValue(emailAddress)
        EuPidCredentialClaimDefinition.MOBILE_PHONE_NUMBER -> Attribute.fromValue(mobilePhoneNumber)
        EuPidCredentialClaimDefinition.TRUST_ANCHOR -> Attribute.fromValue(trustAnchor)
        EuPidCredentialClaimDefinition.LOCATION_STATUS -> Attribute.fromValue(locationStatus)
    }
}

@Serializable
private data class EuPidCredentialVcPayload(
    @SerialName("id")
    val id: String = "",
    @SerialName(Attributes.FAMILY_NAME)
    val familyName: String,
    @SerialName(Attributes.GIVEN_NAME)
    val givenName: String,
    @SerialName(Attributes.BIRTH_DATE)
    @Serializable(with = LocalDateIso8601Serializer::class)
    val birthDate: LocalDate,
    @SerialName(Attributes.FAMILY_NAME_BIRTH)
    val familyNameBirth: String? = null,
    @SerialName(Attributes.GIVEN_NAME_BIRTH)
    val givenNameBirth: String? = null,
    @SerialName(Attributes.PLACE_OF_BIRTH)
    val placeOfBirth: PlaceOfBirth? = null,
    @SerialName(Attributes.RESIDENT_ADDRESS)
    val residentAddress: String? = null,
    @SerialName(Attributes.RESIDENT_COUNTRY)
    val residentCountry: String? = null,
    @SerialName(Attributes.RESIDENT_STATE)
    val residentState: String? = null,
    @SerialName(Attributes.RESIDENT_CITY)
    val residentCity: String? = null,
    @SerialName(Attributes.RESIDENT_POSTAL_CODE)
    val residentPostalCode: String? = null,
    @SerialName(Attributes.RESIDENT_STREET)
    val residentStreet: String? = null,
    @SerialName(Attributes.RESIDENT_HOUSE_NUMBER)
    val residentHouseNumber: String? = null,
    @SerialName(Attributes.SEX)
    val sex: UInt? = null,
    @SerialName(Attributes.NATIONALITY)
    val nationalityElement: JsonElement? = null,
    @SerialName(Attributes.ISSUANCE_DATE)
    val issuanceDate: LocalDateOrInstant,
    @SerialName(Attributes.EXPIRY_DATE)
    val expiryDate: LocalDateOrInstant,
    @SerialName(Attributes.ISSUING_AUTHORITY)
    val issuingAuthority: String,
    @SerialName(Attributes.DOCUMENT_NUMBER)
    val documentNumber: String? = null,
    @SerialName(Attributes.ISSUING_COUNTRY)
    val issuingCountry: String,
    @SerialName(Attributes.ISSUING_JURISDICTION)
    val issuingJurisdiction: String? = null,
    @SerialName(Attributes.PERSONAL_ADMINISTRATIVE_NUMBER)
    val personalAdministrativeNumber: String? = null,
    @SerialName(Attributes.PORTRAIT)
    val portrait: ByteArray? = null,
    @SerialName(Attributes.EMAIL_ADDRESS)
    val emailAddress: String? = null,
    @SerialName(Attributes.MOBILE_PHONE_NUMBER)
    val mobilePhoneNumber: String? = null,
    @SerialName(Attributes.TRUST_ANCHOR)
    val trustAnchor: String? = null,
    @SerialName(Attributes.LOCATION_STATUS)
    val locationStatus: String? = null,
) {
    val sexAsEnum: IsoIec5218Gender?
        get() = sex?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code } }

    val nationality: String?
        get() = nationalityElement?.let { catchingUnwrapped { it.jsonPrimitive.content }.getOrNull() }

    val nationalities: Collection<String>?
        get() = nationalityElement?.let { catchingUnwrapped { it.jsonArray.map { item -> item.jsonPrimitive.content } }.getOrNull() }
}

private class EuPidCredentialVcAdapter(
    val credentialSubject: EuPidCredentialVcPayload,
    decodePortrait: (ByteArray) -> Result<ImageBitmap>,
    override val scheme: CredentialScheme
) : EuPidCredentialAdapter(decodePortrait) {
    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.PLAIN_JWT

    override val givenName: String
        get() = credentialSubject.givenName

    override val familyName: String
        get() = credentialSubject.familyName

    override val birthDate: LocalDate
        get() = credentialSubject.birthDate

    override val portraitRaw: ByteArray?
        get() = credentialSubject.portrait

    override val residentAddress: String?
        get() = credentialSubject.residentAddress

    override val residentStreet: String?
        get() = credentialSubject.residentStreet

    override val residentCity: String?
        get() = credentialSubject.residentCity

    override val residentPostalCode: String?
        get() = credentialSubject.residentPostalCode

    override val residentHouseNumber: String?
        get() = credentialSubject.residentHouseNumber

    override val residentCountry: String?
        get() = credentialSubject.residentCountry

    override val residentState: String?
        get() = credentialSubject.residentState

    override val sex: String?
        get() = credentialSubject.sexAsEnum?.name

    override val nationality: String?
        get() = credentialSubject.nationality

    override val nationalities: Collection<String>?
        get() = credentialSubject.nationalities
            ?: listOfNotNull(credentialSubject.nationality).ifEmpty { null }

    override val familyNameBirth: String?
        get() = credentialSubject.familyNameBirth

    override val givenNameBirth: String?
        get() = credentialSubject.givenNameBirth

    override val placeOfBirth: PlaceOfBirth?
        get() = credentialSubject.placeOfBirth

    override val issuanceDate: LocalDateOrInstant
        get() = credentialSubject.issuanceDate

    override val expiryDate: LocalDateOrInstant
        get() = credentialSubject.expiryDate

    override val issuingAuthority: String
        get() = credentialSubject.issuingAuthority

    override val documentNumber: String?
        get() = credentialSubject.documentNumber

    override val issuingCountry: String
        get() = credentialSubject.issuingCountry

    override val issuingJurisdiction: String?
        get() = credentialSubject.issuingJurisdiction

    override val personalAdministrativeNumber: String?
        get() = credentialSubject.personalAdministrativeNumber

    override val emailAddress: String?
        get() = credentialSubject.emailAddress

    override val mobilePhoneNumber: String?
        get() = credentialSubject.mobilePhoneNumber

    override val trustAnchor: String?
        get() = credentialSubject.trustAnchor

    override val locationStatus: String?
        get() = credentialSubject.locationStatus

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
            ?.get(SdJwtAttributes.Address.FORMATTED)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_FORMATTED]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_ADDRESS]?.contentOrNull

    override val residentStreet: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAttributes.Address.STREET)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_STREET]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_STREET]?.contentOrNull

    override val residentCity: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAttributes.Address.LOCALITY)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_LOCALITY]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_CITY]?.contentOrNull

    override val residentPostalCode: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAttributes.Address.POSTAL_CODE)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_POSTAL_CODE]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_POSTAL_CODE]?.contentOrNull

    override val residentHouseNumber: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAttributes.Address.HOUSE_NUMBER)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_HOUSE_NUMBER]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_HOUSE_NUMBER]?.contentOrNull

    override val residentCountry: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAttributes.Address.COUNTRY)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_COUNTRY]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_COUNTRY]?.contentOrNull

    override val residentState: String?
        get() = (complexJson?.get(SdJwtAttributes.PREFIX_ADDRESS) as? JsonObject?)
            ?.get(SdJwtAttributes.Address.REGION)?.content()
            ?: attributes[SdJwtAttributes.ADDRESS_REGION]?.contentOrNull
            ?: attributes[Attributes.RESIDENT_STATE]?.contentOrNull

    override val sex: String?
        get() = attributes[SdJwtAttributes.SEX]?.contentOrNull?.toIntOrNull()
            ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code.toUInt() }?.name }
            ?: attributes[Attributes.SEX]?.contentOrNull?.toUIntOrNull()
                ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code }?.name }

    override val nationality: String?
        get() = complexJson?.get(SdJwtAttributes.NATIONALITIES)?.toCollectionOrNull()?.firstOrNull()
            ?: attributes[Attributes.NATIONALITY]?.contentOrNull

    override val nationalities: Collection<String>?
        get() = complexJson?.get(SdJwtAttributes.NATIONALITIES)?.toCollectionOrNull()
            ?: attributes[SdJwtAttributes.NATIONALITIES]?.let { listOfNotNull(it.contentOrNull) }?.ifEmpty { null }
            ?: listOfNotNull(nationality).ifEmpty { null }

    override val familyNameBirth: String?
        get() = attributes[SdJwtAttributes.FAMILY_NAME_BIRTH]?.contentOrNull
            ?: attributes[Attributes.FAMILY_NAME_BIRTH]?.contentOrNull

    override val givenNameBirth: String?
        get() = attributes[SdJwtAttributes.GIVEN_NAME_BIRTH]?.contentOrNull
            ?: attributes[Attributes.GIVEN_NAME_BIRTH]?.contentOrNull

    override val placeOfBirth: PlaceOfBirth?
        get() = attributes[SdJwtAttributes.PREFIX_PLACE_OF_BIRTH]?.let {
            catchingUnwrapped { joseCompliantSerializer.decodeFromJsonElement<PlaceOfBirth>(it) }.getOrNull()
        }

    override val issuanceDate: LocalDateOrInstant?
        get() = attributes[SdJwtAttributes.ISSUANCE_DATE]?.contentOrNull?.toLocalDateOrInstantOrNull()
            ?: attributes[Attributes.ISSUANCE_DATE]?.contentOrNull?.toLocalDateOrInstantOrNull()

    override val expiryDate: LocalDateOrInstant?
        get() = attributes[SdJwtAttributes.EXPIRY_DATE]?.contentOrNull?.toLocalDateOrInstantOrNull()
            ?: attributes[Attributes.EXPIRY_DATE]?.contentOrNull?.toLocalDateOrInstantOrNull()

    override val issuingAuthority: String?
        get() = attributes[SdJwtAttributes.ISSUING_AUTHORITY]?.contentOrNull
            ?: attributes[Attributes.ISSUING_AUTHORITY]?.contentOrNull

    override val documentNumber: String?
        get() = attributes[SdJwtAttributes.DOCUMENT_NUMBER]?.contentOrNull
            ?: attributes[Attributes.DOCUMENT_NUMBER]?.contentOrNull

    override val issuingCountry: String?
        get() = attributes[SdJwtAttributes.ISSUING_COUNTRY]?.contentOrNull
            ?: attributes[Attributes.ISSUING_COUNTRY]?.contentOrNull

    override val issuingJurisdiction: String?
        get() = attributes[SdJwtAttributes.ISSUING_JURISDICTION]?.contentOrNull
            ?: attributes[Attributes.ISSUING_JURISDICTION]?.contentOrNull

    override val personalAdministrativeNumber: String?
        get() = attributes[SdJwtAttributes.PERSONAL_ADMINISTRATIVE_NUMBER]?.contentOrNull
            ?: attributes[Attributes.PERSONAL_ADMINISTRATIVE_NUMBER]?.contentOrNull

    override val emailAddress: String?
        get() = attributes[SdJwtAttributes.EMAIL]?.contentOrNull
            ?: attributes[Attributes.EMAIL_ADDRESS]?.contentOrNull

    override val mobilePhoneNumber: String?
        get() = attributes[SdJwtAttributes.PHONE_NUMBER]?.contentOrNull
            ?: attributes[Attributes.MOBILE_PHONE_NUMBER]?.contentOrNull

    override val trustAnchor: String?
        get() = attributes[SdJwtAttributes.TRUST_ANCHOR]?.contentOrNull
            ?: attributes[Attributes.TRUST_ANCHOR]?.contentOrNull

    override val locationStatus: String?
        get() = attributes[Attributes.LOCATION_STATUS]?.contentOrNull

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
                is String -> it.decodeBase64Bytes()
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

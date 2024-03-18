package data

import Resources
import at.asitplus.wallet.eupid.EuPidCredential
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.jsonSerializer
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_at_least_14
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_at_least_16
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_at_least_18
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_at_least_21
import composewalletapp.shared.generated.resources.attribute_friendly_name_bpk
import composewalletapp.shared.generated.resources.attribute_friendly_name_date_of_birth
import composewalletapp.shared.generated.resources.attribute_friendly_name_firstname
import composewalletapp.shared.generated.resources.attribute_friendly_name_lastname
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_address
import composewalletapp.shared.generated.resources.attribute_friendly_name_portrait
import composewalletapp.shared.generated.resources.Res
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.decodeBase64String
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource

private val SubjectCredentialStore.StoreEntry.Vc.unsupportedCredentialSubjectMessage: String
    get() = "Unsupported credential subject: ${this.vc.vc.credentialSubject}"

private val SubjectCredentialStore.StoreEntry.SdJwt.unsupportedCredentialSchemeMessage: String
    get() = "Unsupported credential scheme: ${this.scheme}"

private val SubjectCredentialStore.StoreEntry.Iso.unsupportedCredentialSchemeMessage: String
    get() = "Unsupported credential scheme: ${this.scheme}"

private val SubjectCredentialStore.StoreEntry.unsupportedCredentialStoreEntry: String
    get() = "Unsupported credential store entry: $this"

////////////////////////////////////////////////////////////////////////////////////////////////////

@OptIn(ExperimentalResourceApi::class)
val String.attributeTranslation: StringResource
    get() = when (this) {
        IdAustriaScheme.Attributes.BPK -> Res.string.attribute_friendly_name_bpk
        IdAustriaScheme.Attributes.FIRSTNAME -> Res.string.attribute_friendly_name_firstname
        IdAustriaScheme.Attributes.LASTNAME -> Res.string.attribute_friendly_name_lastname
        IdAustriaScheme.Attributes.DATE_OF_BIRTH -> Res.string.attribute_friendly_name_date_of_birth
        IdAustriaScheme.Attributes.PORTRAIT -> Res.string.attribute_friendly_name_portrait
        IdAustriaScheme.Attributes.AGE_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
        IdAustriaScheme.Attributes.AGE_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
        IdAustriaScheme.Attributes.AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
        IdAustriaScheme.Attributes.AGE_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
        IdAustriaScheme.Attributes.MAIN_ADDRESS -> Res.string.attribute_friendly_name_main_address

        EuPidScheme.Attributes.GIVEN_NAME -> Resources.attribute_friendly_name_firstname
        EuPidScheme.Attributes.FAMILY_NAME -> Resources.attribute_friendly_name_lastname
        EuPidScheme.Attributes.BIRTH_DATE -> Resources.attribute_friendly_name_date_of_birth
        EuPidScheme.Attributes.AGE_OVER_18 -> Resources.attribute_friendly_name_age_at_least_18
        EuPidScheme.Attributes.RESIDENT_ADDRESS -> Resources.attribute_friendly_name_main_address

        else -> throw Exception("Unsupported IdAustria attribute name: $this")
    }

@Serializable
private data class IdAustriaMainAddressAdapter(
    val Gemeindekennziffer: String? = null,
    val Gemeindebezeichnung: String? = null,
    val Postleitzahl: String? = null,
    val Ortschaft: String? = null,
    val Strasse: String? = null,
    val Hausnummer: String? = null,
    val Stiege: String? = null,
    val Tuer: String? = null,
)

class CredentialExtractor(
    private val credentials: List<SubjectCredentialStore.StoreEntry>,
) {
    // TODO: might not contain sufficient context information
    fun getAttributeByName(attributeName: String): Any? {
        return when (attributeName) {
            IdAustriaScheme.Attributes.FIRSTNAME -> this.givenName
            IdAustriaScheme.Attributes.LASTNAME -> this.familyName
            IdAustriaScheme.Attributes.DATE_OF_BIRTH -> this.dateOfBirth
            IdAustriaScheme.Attributes.PORTRAIT -> this.portrait
            IdAustriaScheme.Attributes.AGE_OVER_14 -> this.ageAtLeast14
            IdAustriaScheme.Attributes.AGE_OVER_16 -> this.ageAtLeast16
            IdAustriaScheme.Attributes.AGE_OVER_18 -> this.ageAtLeast18
            IdAustriaScheme.Attributes.AGE_OVER_21 -> this.ageAtLeast21
            IdAustriaScheme.Attributes.MAIN_ADDRESS -> this.idAustriaCredentialMainAddressAdapter

            EuPidScheme.Attributes.GIVEN_NAME -> this.givenName
            EuPidScheme.Attributes.FAMILY_NAME -> this.familyName
            EuPidScheme.Attributes.BIRTH_DATE -> this.dateOfBirth
            EuPidScheme.Attributes.AGE_OVER_18 -> this.ageAtLeast18
            EuPidScheme.Attributes.RESIDENT_STREET -> this.mainResidenceStreetName
            EuPidScheme.Attributes.RESIDENT_CITY -> this.mainResidenceTownName
            EuPidScheme.Attributes.RESIDENT_POSTAL_CODE -> this.mainResidencePostalCode
            EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER -> this.mainResidenceHouseNumber
            EuPidScheme.Attributes.RESIDENT_COUNTRY -> this.mainResidenceCountryName
            EuPidScheme.Attributes.RESIDENT_STATE -> this.mainResidenceStateName

            else -> null
        }
    }

    // TODO: might not contain sufficient context information
    fun containsAttribute(attributeName: String): Boolean {
        return when (attributeName) {
            IdAustriaScheme.Attributes.FIRSTNAME -> this.givenName != null
            IdAustriaScheme.Attributes.LASTNAME -> this.familyName != null
            IdAustriaScheme.Attributes.DATE_OF_BIRTH -> this.dateOfBirth != null
            IdAustriaScheme.Attributes.PORTRAIT -> this.portrait != null
            IdAustriaScheme.Attributes.AGE_OVER_14 -> this.ageAtLeast14 != null
            IdAustriaScheme.Attributes.AGE_OVER_16 -> this.ageAtLeast16 != null
            IdAustriaScheme.Attributes.AGE_OVER_18 -> this.ageAtLeast18 != null
            IdAustriaScheme.Attributes.AGE_OVER_21 -> this.ageAtLeast21 != null
            IdAustriaScheme.Attributes.MAIN_ADDRESS -> this.idAustriaCredentialMainAddressAdapter != null

            EuPidScheme.Attributes.GIVEN_NAME -> this.givenName != null
            EuPidScheme.Attributes.FAMILY_NAME -> this.familyName != null
            EuPidScheme.Attributes.BIRTH_DATE -> this.dateOfBirth != null
            EuPidScheme.Attributes.AGE_OVER_18 -> this.ageAtLeast18 != null
            EuPidScheme.Attributes.RESIDENT_STREET -> this.mainResidenceStreetName != null
            EuPidScheme.Attributes.RESIDENT_CITY -> this.mainResidenceTownName != null
            EuPidScheme.Attributes.RESIDENT_POSTAL_CODE -> this.mainResidencePostalCode != null
            EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER -> this.mainResidenceHouseNumber != null
            EuPidScheme.Attributes.RESIDENT_COUNTRY -> this.mainResidenceCountryName != null
            EuPidScheme.Attributes.RESIDENT_STATE -> this.mainResidenceStateName != null

            else -> false
        }
    }

    val givenName: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.firstname

                        is EuPidCredential -> credentialSubject.givenName

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.FIRSTNAME }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }


                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.GIVEN_NAME }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }


                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.FIRSTNAME
                        }?.value?.elementValue?.string

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.GIVEN_NAME
                        }?.value?.elementValue?.string

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val familyName: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.lastname

                        is EuPidCredential -> credentialSubject.familyName

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.LASTNAME }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.FAMILY_NAME }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.LASTNAME
                        }?.value?.elementValue?.string

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.FAMILY_NAME
                        }?.value?.elementValue?.string

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val dateOfBirth: LocalDate?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.dateOfBirth

                        is EuPidCredential -> credentialSubject.birthDate

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.DATE_OF_BIRTH }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.let { LocalDate.parse(it) }

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.BIRTH_DATE }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.let { LocalDate.parse(it) }

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.DATE_OF_BIRTH
                        }?.value?.elementValue?.date

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.BIRTH_DATE
                        }?.value?.elementValue?.date

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val portrait: ByteArray?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.portrait

                        is EuPidCredential -> null

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.PORTRAIT }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.decodeBase64Bytes()

                        is EuPidScheme -> null

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.PORTRAIT
                        }?.value?.elementValue?.bytes

                        is EuPidScheme -> null

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val ageAtLeast14: Boolean?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.ageOver14

                        is EuPidCredential -> null

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_14 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }

                        is EuPidScheme -> null

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.AGE_OVER_14
                        }?.value?.elementValue?.boolean

                        is EuPidScheme -> null

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val ageAtLeast16: Boolean?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.ageOver16

                        is EuPidCredential -> null

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_16 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }

                        is EuPidScheme -> null

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.AGE_OVER_16
                        }?.value?.elementValue?.boolean

                        is EuPidScheme -> null

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val ageAtLeast18: Boolean?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.ageOver18

                        is EuPidCredential -> credentialSubject.ageOver18

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_18 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.AGE_OVER_18 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.AGE_OVER_18
                        }?.value?.elementValue?.boolean

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.AGE_OVER_18
                        }?.value?.elementValue?.boolean

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val ageAtLeast21: Boolean?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.ageOver21

                        is EuPidCredential -> null

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_21 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }

                        is EuPidScheme -> null

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.AGE_OVER_21
                        }?.value?.elementValue?.boolean

                        is EuPidScheme -> null

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    private val idAustriaCredentialMainAddressAdapter: IdAustriaMainAddressAdapter?
        get() = credentials.firstNotNullOfOrNull {
            it.idAustriaCredentialMainAddressAdapter
        }

    private val SubjectCredentialStore.StoreEntry.idAustriaCredentialMainAddressAdapter: IdAustriaMainAddressAdapter?
        get() = when (val credential = this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = credential.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> credentialSubject.mainAddress

                    else -> null
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (credential.scheme) {
                    is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.MAIN_ADDRESS }
                        .firstNotNullOfOrNull { it.value?.claimValue as String }

                    else -> null
                }
            }

            is SubjectCredentialStore.StoreEntry.Iso -> {
                when (credential.scheme) {
                    is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                        IdAustriaScheme.isoNamespace
                    )?.entries?.firstOrNull {
                        it.value.elementIdentifier == IdAustriaScheme.Attributes.MAIN_ADDRESS
                    }?.value?.elementValue?.string

                    else -> null
                }
            }

            else -> null
        }?.decodeBase64String()?.let {
            jsonSerializer.decodeFromString<IdAustriaMainAddressAdapter>(it)
        }

    val mainResidenceStreetName: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credential.idAustriaCredentialMainAddressAdapter?.Strasse

                        is EuPidCredential -> credentialSubject.residentStreet

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Strasse

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.RESIDENT_STREET }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Strasse

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_STREET
                        }?.value?.elementValue?.string

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val mainResidenceHouseNumber: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credential.idAustriaCredentialMainAddressAdapter?.Hausnummer

                        is EuPidCredential -> credentialSubject.residentHouseNumber

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Hausnummer

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Hausnummer

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER
                        }?.value?.elementValue?.string

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val mainResidenceStairName: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credential.idAustriaCredentialMainAddressAdapter?.Stiege

                        is EuPidCredential -> null

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Stiege

                        is EuPidScheme -> null

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Stiege

                        is EuPidScheme -> null

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val mainResidenceDoorName: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credential.idAustriaCredentialMainAddressAdapter?.Tuer

                        is EuPidCredential -> null

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Tuer

                        is EuPidScheme -> null

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Tuer

                        is EuPidScheme -> null

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val mainResidenceTownName: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credential.idAustriaCredentialMainAddressAdapter?.Ortschaft

                        is EuPidCredential -> credentialSubject.residentCity

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Ortschaft

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.RESIDENT_CITY }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Ortschaft

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_CITY
                        }?.value?.elementValue?.string

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val mainResidencePostalCode: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credential.idAustriaCredentialMainAddressAdapter?.Postleitzahl

                        is EuPidCredential -> credentialSubject.residentPostalCode

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Postleitzahl

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.RESIDENT_POSTAL_CODE }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Postleitzahl

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_POSTAL_CODE
                        }?.value?.elementValue?.string

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val mainResidenceStateName: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.residentState

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.RESIDENT_STATE }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_STATE
                        }?.value?.elementValue?.string

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val mainResidenceCountryName: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.residentCountry

                        else -> TODO(credential.unsupportedCredentialSubjectMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.RESIDENT_COUNTRY }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_COUNTRY
                        }?.value?.elementValue?.string

                        else -> TODO(credential.unsupportedCredentialSchemeMessage)
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }
}
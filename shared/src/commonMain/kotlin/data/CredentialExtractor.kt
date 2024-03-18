package data

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

private val SubjectCredentialStore.StoreEntry.unsupportedCredentialStoreEntry: String
    get() = "Unsupported credential store entry: $this"

////////////////////////////////////////////////////////////////////////////////////////////////////

@OptIn(ExperimentalResourceApi::class)
val String.idAustriaAttributeTranslation: StringResource
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
        else -> throw Exception("Unsupported IdAustria attribute name: $this")
    }

@Serializable
private data class MainAddressAdapter(
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
    credentials: List<SubjectCredentialStore.StoreEntry>,
) {
    fun containsIdAustriaAttribute(attributeName: String): Boolean {
        return when (attributeName) {
            IdAustriaScheme.Attributes.FIRSTNAME -> this.firstname != null
            IdAustriaScheme.Attributes.LASTNAME -> this.lastname != null
            IdAustriaScheme.Attributes.DATE_OF_BIRTH -> this.dateOfBirth != null
            IdAustriaScheme.Attributes.PORTRAIT -> this.portrait != null
            IdAustriaScheme.Attributes.AGE_OVER_14 -> this.ageAtLeast14 != null
            IdAustriaScheme.Attributes.AGE_OVER_16 -> this.ageAtLeast16 != null
            IdAustriaScheme.Attributes.AGE_OVER_18 -> this.ageAtLeast18 != null
            IdAustriaScheme.Attributes.AGE_OVER_21 -> this.ageAtLeast21 != null
            IdAustriaScheme.Attributes.MAIN_ADDRESS -> this.mainAddress != null
            else -> false
        }
    }

    val firstname: String? = credentials.firstNotNullOfOrNull { credential ->
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = credential.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.firstname
                    }

                    else -> TODO(credential.unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (credential.scheme) {
                    is IdAustriaScheme -> {
                        credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.FIRSTNAME }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                    }

                    else -> TODO(credential.unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(credential.unsupportedCredentialStoreEntry)
        }
    }

    val lastname: String? = credentials.firstNotNullOfOrNull { credential ->
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = credential.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.lastname
                    }

                    else -> TODO(credential.unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (credential.scheme) {
                    is IdAustriaScheme -> {
                        credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.LASTNAME }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                    }

                    else -> TODO(credential.unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(credential.unsupportedCredentialStoreEntry)
        }
    }

    val dateOfBirth: LocalDate? = credentials.firstNotNullOfOrNull { credential ->
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = credential.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.dateOfBirth
                    }

                    else -> TODO(credential.unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (credential.scheme) {
                    is IdAustriaScheme -> {
                        credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.DATE_OF_BIRTH }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.let {
                                LocalDate.parse(it)
                            }
                    }

                    else -> TODO(credential.unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(credential.unsupportedCredentialStoreEntry)
        }
    }

    val portrait: ByteArray? = credentials.firstNotNullOfOrNull { credential ->
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = credential.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.portrait
                    }

                    else -> TODO(credential.unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (credential.scheme) {
                    is IdAustriaScheme -> {
                        credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.PORTRAIT }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.decodeBase64Bytes()
                    }

                    else -> TODO(credential.unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(credential.unsupportedCredentialStoreEntry)
        }
    }

    val ageAtLeast14: Boolean? = credentials.firstNotNullOfOrNull { credential ->
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = credential.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.ageOver14
                    }

                    else -> TODO(credential.unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (credential.scheme) {
                    is IdAustriaScheme -> {
                        credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_14 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }
                    }

                    else -> TODO(credential.unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(credential.unsupportedCredentialStoreEntry)
        }
    }

    val ageAtLeast16: Boolean? = credentials.firstNotNullOfOrNull { credential ->
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = credential.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.ageOver16
                    }

                    else -> TODO(credential.unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (credential.scheme) {
                    is IdAustriaScheme -> {
                        credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_16 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }
                    }

                    else -> TODO(credential.unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(credential.unsupportedCredentialStoreEntry)
        }
    }

    val ageAtLeast18: Boolean? = credentials.firstNotNullOfOrNull { credential ->
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = credential.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.ageOver18
                    }

                    else -> TODO(credential.unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (credential.scheme) {
                    is IdAustriaScheme -> {
                        credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_18 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }
                    }

                    else -> TODO(credential.unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(credential.unsupportedCredentialStoreEntry)
        }
    }

    val ageAtLeast21: Boolean? = credentials.firstNotNullOfOrNull { credential ->
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = credential.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.ageOver21
                    }

                    else -> TODO(credential.unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (credential.scheme) {
                    is IdAustriaScheme -> {
                        credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_21 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }
                    }

                    else -> TODO(credential.unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(credential.unsupportedCredentialStoreEntry)
        }
    }

    private val mainAddressBase64: String? = credentials.firstNotNullOfOrNull { credential ->
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = credential.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> credentialSubject.mainAddress

                    else -> TODO(credential.unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (credential.scheme) {
                    is IdAustriaScheme -> {
                        credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.MAIN_ADDRESS }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                    }

                    else -> TODO(credential.unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(credential.unsupportedCredentialStoreEntry)
        }
    }

    private val mainAddressJson: String? = mainAddressBase64?.decodeBase64String()

    private val mainAddress: MainAddressAdapter? = mainAddressJson?.let {
        jsonSerializer.decodeFromString<MainAddressAdapter>(it)
    }

    val mainAddressStreetName: String? = mainAddress?.Strasse

    val mainAddressHouseNumber: String? = mainAddress?.Hausnummer

    val mainAddressStair: String? = mainAddress?.Stiege

    val mainAddressDoor: String? = mainAddress?.Tuer

    val mainAddressVillageName: String? = mainAddress?.Ortschaft

    val mainAddressPostalCode: String? = mainAddress?.Postleitzahl
}
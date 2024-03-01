package data

import Resources
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import io.ktor.util.decodeBase64Bytes
import kotlinx.datetime.LocalDate

private val SubjectCredentialStore.StoreEntry.Vc.unsupportedCredentialSubjectMessage: String
    get() = "Unsupported credential subject: ${this.vc.vc.credentialSubject}"

private val SubjectCredentialStore.StoreEntry.SdJwt.unsupportedCredentialSchemeMessage: String
    get() = "Unsupported credential scheme: ${this.scheme}"

private val SubjectCredentialStore.StoreEntry.unsupportedCredentialStoreEntry: String
    get() = "Unsupported credential store entry: $this"

////////////////////////////////////////////////////////////////////////////////////////////////////

val String.idAustriaAttributeTranslation: String
    get() = when (this) {
        IdAustriaScheme.Attributes.BPK -> Resources.ATTRIBUTE_FRIENDLY_NAME_BPK
        IdAustriaScheme.Attributes.FIRSTNAME -> Resources.ATTRIBUTE_FRIENDLY_NAME_FIRSTNAME
        IdAustriaScheme.Attributes.LASTNAME -> Resources.ATTRIBUTE_FRIENDLY_NAME_LASTNAME
        IdAustriaScheme.Attributes.DATE_OF_BIRTH -> Resources.ATTRIBUTE_FRIENDLY_NAME_DATE_OF_BIRTH
        IdAustriaScheme.Attributes.PORTRAIT -> Resources.ATTRIBUTE_FRIENDLY_NAME_PORTRAIT
        IdAustriaScheme.Attributes.AGE_OVER_14 -> Resources.ATTRIBUTE_FRIENDLY_NAME_AGE_AT_LEAST_14
        IdAustriaScheme.Attributes.AGE_OVER_16 -> Resources.ATTRIBUTE_FRIENDLY_NAME_AGE_AT_LEAST_16
        IdAustriaScheme.Attributes.AGE_OVER_18 -> Resources.ATTRIBUTE_FRIENDLY_NAME_AGE_AT_LEAST_18
        IdAustriaScheme.Attributes.AGE_OVER_21 -> Resources.ATTRIBUTE_FRIENDLY_NAME_AGE_AT_LEAST_21
        IdAustriaScheme.Attributes.MAIN_ADDRESS -> Resources.ATTRIBUTE_FRIENDLY_NAME_MAIN_ADDRESS
        else -> throw Exception("Unsupported IdAustria attribute name: $this")
    }

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

    val mainAddress: String? = credentials.firstNotNullOfOrNull { credential ->
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = credential.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.mainAddress
                    }

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
}
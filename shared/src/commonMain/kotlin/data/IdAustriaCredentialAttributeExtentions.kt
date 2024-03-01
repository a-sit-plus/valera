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

fun SubjectCredentialStore.StoreEntry.containsIdAustriaAttribute(attributeName: String): Boolean {
    return when (attributeName) {
        IdAustriaScheme.Attributes.BPK -> this.bpk != null
        IdAustriaScheme.Attributes.FIRSTNAME -> this.firstname != null
        IdAustriaScheme.Attributes.LASTNAME -> this.lastname != null
        IdAustriaScheme.Attributes.DATE_OF_BIRTH -> this.dateOfBirth != null
        IdAustriaScheme.Attributes.PORTRAIT -> this.portrait != null
        IdAustriaScheme.Attributes.AGE_OVER_14 -> this.ageAtLeast14 == true
        IdAustriaScheme.Attributes.AGE_OVER_16 -> this.ageAtLeast16 == true
        IdAustriaScheme.Attributes.AGE_OVER_18 -> this.ageAtLeast18 == true
        IdAustriaScheme.Attributes.AGE_OVER_21 -> this.ageAtLeast21 == true
        IdAustriaScheme.Attributes.MAIN_ADDRESS -> this.mainAddress != null
        else -> false
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.bpk: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = this.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.bpk
                    }

                    else -> TODO(unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (this.scheme) {
                    is IdAustriaScheme -> {
                        this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.BPK }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                    }

                    else -> TODO(unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(unsupportedCredentialStoreEntry)
        }
    }

val SubjectCredentialStore.StoreEntry.firstname: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = this.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.firstname
                    }

                    else -> TODO(unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (this.scheme) {
                    is IdAustriaScheme -> {
                        this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.FIRSTNAME }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                    }

                    else -> TODO(unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(unsupportedCredentialStoreEntry)
        }
    }

val SubjectCredentialStore.StoreEntry.lastname: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = this.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.lastname
                    }

                    else -> TODO(unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (this.scheme) {
                    is IdAustriaScheme -> {
                        this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.LASTNAME }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                    }

                    else -> TODO(unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(unsupportedCredentialStoreEntry)
        }
    }

val SubjectCredentialStore.StoreEntry.dateOfBirth: LocalDate?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = this.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.dateOfBirth
                    }

                    else -> TODO(unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (this.scheme) {
                    is IdAustriaScheme -> {
                        this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.DATE_OF_BIRTH }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.let {
                                LocalDate.parse(it)
                            }
                    }

                    else -> TODO(unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(unsupportedCredentialStoreEntry)
        }
    }

val SubjectCredentialStore.StoreEntry.portrait: ByteArray?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = this.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.portrait
                    }

                    else -> TODO(unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (this.scheme) {
                    is IdAustriaScheme -> {
                        this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.PORTRAIT }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.decodeBase64Bytes()
                    }

                    else -> TODO(unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(unsupportedCredentialStoreEntry)
        }
    }

val SubjectCredentialStore.StoreEntry.ageAtLeast14: Boolean?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = this.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.ageOver14
                    }

                    else -> TODO(unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (this.scheme) {
                    is IdAustriaScheme -> {
                        this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_14 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }
                    }

                    else -> TODO(unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(unsupportedCredentialStoreEntry)
        }
    }

val SubjectCredentialStore.StoreEntry.ageAtLeast16: Boolean?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = this.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.ageOver16
                    }

                    else -> TODO(unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (this.scheme) {
                    is IdAustriaScheme -> {
                        this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_16 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }
                    }

                    else -> TODO(unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(unsupportedCredentialStoreEntry)
        }
    }

val SubjectCredentialStore.StoreEntry.ageAtLeast18: Boolean?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = this.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.ageOver18
                    }

                    else -> TODO(unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (this.scheme) {
                    is IdAustriaScheme -> {
                        this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_18 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }
                    }

                    else -> TODO(unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(unsupportedCredentialStoreEntry)
        }
    }

val SubjectCredentialStore.StoreEntry.ageAtLeast21: Boolean?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = this.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.ageOver21
                    }

                    else -> TODO(unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (this.scheme) {
                    is IdAustriaScheme -> {
                        this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_21 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }
                    }

                    else -> TODO(unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(unsupportedCredentialStoreEntry)
        }
    }

val SubjectCredentialStore.StoreEntry.mainAddress: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                when (val credentialSubject = this.vc.vc.credentialSubject) {
                    is IdAustriaCredential -> {
                        credentialSubject.mainAddress
                    }

                    else -> TODO(unsupportedCredentialSubjectMessage)
                }
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                when (this.scheme) {
                    is IdAustriaScheme -> {
                        this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.MAIN_ADDRESS }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                    }

                    else -> TODO(unsupportedCredentialSchemeMessage)
                }
            }

            else -> TODO(unsupportedCredentialStoreEntry)
        }
    }
package data

import Resources
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import io.github.aakira.napier.Napier
import io.ktor.util.decodeBase64Bytes
import kotlinx.datetime.LocalDate

private val SubjectCredentialStore.StoreEntry.Vc.invalidCredentialSubjectMessage: String
    get() = "Unsupported credential subject: ${this.vc.vc.credentialSubject}"

private val SubjectCredentialStore.StoreEntry.SdJwt.invalidCredentialSchemeMessage: String
    get() = "Unsupported credential scheme: ${this.scheme}"

private val SubjectCredentialStore.StoreEntry.invalidCredentialStoreEntry: String
    get() = "Unsupported credential store entry: $this"

//////////////////////////////////////////////////////////////////////////////////////////

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

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.bpk: String
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                credentialSubject.bpk
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.bpk: String?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.BPK }
                    .firstNotNullOfOrNull { it.value?.claimValue as String }
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.bpk: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.bpk
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.bpk
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.firstname: String
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                credentialSubject.firstname
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.firstname: String?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.FIRSTNAME }
                    .firstNotNullOfOrNull { it.value?.claimValue as String }
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.firstname: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.firstname
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.firstname
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.lastname: String
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                credentialSubject.lastname
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.lastname: String?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.LASTNAME }
                    .firstNotNullOfOrNull { it.value?.claimValue as String }
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.lastname: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.lastname
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.lastname
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.dateOfBirth: LocalDate
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                credentialSubject.dateOfBirth
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.dateOfBirth: LocalDate?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.DATE_OF_BIRTH }
                    .firstNotNullOfOrNull { it.value?.claimValue as String }
                    ?.let {
                        LocalDate.parse(it)
                    }
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.dateOfBirth: LocalDate?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.dateOfBirth
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.dateOfBirth
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.portrait: ByteArray?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                credentialSubject.portrait
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.portrait: ByteArray?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.PORTRAIT }
                    .firstNotNullOfOrNull { it.value?.claimValue as String }?.decodeBase64Bytes()
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.portrait: ByteArray?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.portrait
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.portrait
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.ageAtLeast14: Boolean?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                credentialSubject.ageOver14
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.ageAtLeast14: Boolean?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_14 }
                    .firstNotNullOfOrNull { it.value?.claimValue as Boolean }
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.ageAtLeast14: Boolean?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.ageAtLeast14
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.ageAtLeast14
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.ageAtLeast16: Boolean?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                credentialSubject.ageOver16
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.ageAtLeast16: Boolean?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_16 }
                    .firstNotNullOfOrNull { it.value?.claimValue as Boolean }
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.ageAtLeast16: Boolean?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.ageAtLeast16
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.ageAtLeast16
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.ageAtLeast18: Boolean?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                credentialSubject.ageOver18
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.ageAtLeast18: Boolean?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_18 }
                    .firstNotNullOfOrNull { it.value?.claimValue as Boolean }
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.ageAtLeast18: Boolean?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.ageAtLeast18
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.ageAtLeast18
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.ageAtLeast21: Boolean?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                credentialSubject.ageOver21
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.ageAtLeast21: Boolean?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_21 }
                    .firstNotNullOfOrNull { it.value?.claimValue as Boolean }
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.ageAtLeast21: Boolean?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.ageAtLeast21
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.ageAtLeast21
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.drivingPermissions: List<String>
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                Napier.w("Placeholder attribute has been accessed: SubjectCredentialStore.StoreEntry.Vc.drivingPermissions")
                listOf(
                    "valuePlacheholder1For:SubjectCredentialStore.StoreEntry.Vc.drivingPermissions",
                    "valuePlacheholder2For:SubjectCredentialStore.StoreEntry.Vc.drivingPermissions"
                )
//                TODO("Missing Implementation")
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.drivingPermissions: List<String>
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                Napier.w("Placeholder attribute has been accessed: SubjectCredentialStore.StoreEntry.SdJwt.drivingPermissions")
                listOf(
                    "valuePlacheholder1For:SubjectCredentialStore.StoreEntry.SdJwt.drivingPermissions",
                    "valuePlacheholder2For:SubjectCredentialStore.StoreEntry.SdJwt.drivingPermissions"
                )
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.drivingPermissions: List<String>
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.drivingPermissions
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.drivingPermissions
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.mainAddress: String?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                credentialSubject.mainAddress
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.mainAddress: String?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                this.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.MAIN_ADDRESS }
                    .firstNotNullOfOrNull { it.value?.claimValue as String }
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.mainAddress: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.mainAddress
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.mainAddress
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.carModel: String?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                Napier.w("Placeholder attribute has been accessed: SubjectCredentialStore.StoreEntry.Vc.carModel")
                "valuePlaceholderFor:SubjectCredentialStore.StoreEntry.Vc.carModel"
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.carModel: String?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                Napier.w("Placeholder attribute has been accessed: SubjectCredentialStore.StoreEntry.SdJwt.carModel")
                "valuePlaceholderFor:SubjectCredentialStore.StoreEntry.SdJwt.carModel"
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.carModel: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.carModel
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.carModel
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.licensePlateNumber: String?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                Napier.w("Placeholder attribute has been accessed: SubjectCredentialStore.StoreEntry.Vc.licensePlateNumber")
                "valuePlaceholderFor:SubjectCredentialStore.StoreEntry.Vc.licensePlateNumber"
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.licensePlateNumber: String?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                Napier.w("Placeholder attribute has been accessed: SubjectCredentialStore.StoreEntry.SdJwt.licensePlateNumber")
                "valuePlaceholderFor:SubjectCredentialStore.StoreEntry.SdJwt.licensePlateNumber"
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.licensePlateNumber: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.licensePlateNumber
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.licensePlateNumber
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }
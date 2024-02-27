package data

import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import kotlinx.datetime.LocalDate

private val SubjectCredentialStore.StoreEntry.Vc.invalidCredentialSubjectMessage: String
    get() = "Unsupported credential subject: ${this.vc.vc.credentialSubject}"

private val SubjectCredentialStore.StoreEntry.SdJwt.invalidCredentialSchemeMessage: String
    get() = "Unsupported credential scheme: ${this.scheme}"

private val SubjectCredentialStore.StoreEntry.invalidCredentialStoreEntry: String
    get() = "Unsupported credential store entry: $this"

//////////////////////////////////////////////////////////////////////////////////////////
// get from claim name
//////////////////////////////////////////////////////////////////////////////////////////

enum class IdAustriaAttribute(val attributeName: String, val deTranslation: String) {
    FirstName(attributeName = "firstname", deTranslation = "Vorname"),
    LastName(attributeName = "lastname", deTranslation = "Nachname"),
    DateOfBirth(attributeName = "date-of-birth", deTranslation = "Geburtsdatum"),
    Portrait(attributeName = "dummyPortraitAttributeName", deTranslation = "Portrait"),
    AgeAtLeast14(attributeName = "dummyAgeAtLeast14AttributeName", deTranslation = "Zumindest 14 Jahre alt"),
    AgeAtLeast16(attributeName = "dummyAgeAtLeast16AttributeName", deTranslation = "Zumindest 16 Jahre alt"),
    AgeAtLeast18(attributeName = "dummyAgeAtLeast18AttributeName", deTranslation = "Zumindest 18 Jahre alt"),
    AgeAtLeast21(attributeName = "dummyAgeAtLeast21AttributeName", deTranslation = "Zumindest 21 Jahre alt"),
    DrivingPermissions(attributeName = "dummyDrivingPermissionsAttributeName", deTranslation = "Vorname"),
    StreetName(attributeName = "dummyStreetNameAttributeName", deTranslation = "Vorname"),
    PostalCode(attributeName = "dummyPostalCodeAttributeName", deTranslation = "Vorname"),
    TownName(attributeName = "dummyTownNameAttributeName", deTranslation = "Vorname"),
    CarModel(attributeName = "dummyCarModelAttributeName", deTranslation = "Vorname"),
    LicensePlateNumber(attributeName = "dummyLicensePlateNumberAttributeName", deTranslation = "Vorname"),
    ;

    companion object {
        fun attributeTranslation(attributeName: String): String {
            return when (attributeName) {
                FirstName.attributeName -> FirstName.deTranslation
                LastName.attributeName -> LastName.deTranslation
                DateOfBirth.attributeName -> DateOfBirth.deTranslation
                Portrait.attributeName -> Portrait.deTranslation
                AgeAtLeast14.attributeName -> AgeAtLeast14.deTranslation
                AgeAtLeast16.attributeName -> AgeAtLeast16.deTranslation
                AgeAtLeast18.attributeName -> AgeAtLeast18.deTranslation
                AgeAtLeast21.attributeName -> AgeAtLeast21.deTranslation
                DrivingPermissions.attributeName -> DrivingPermissions.deTranslation
                StreetName.attributeName -> StreetName.deTranslation
                PostalCode.attributeName -> PostalCode.deTranslation
                TownName.attributeName -> TownName.deTranslation
                CarModel.attributeName -> CarModel.deTranslation
                LicensePlateNumber.attributeName -> LicensePlateNumber.deTranslation
                else -> throw Exception("Unsupported Attribute Name: $attributeName")
            }
        }
    }
}

fun SubjectCredentialStore.StoreEntry.containsIdAustriaAttribute(attributeName: String): Boolean {
        return when (attributeName) {
            IdAustriaAttribute.FirstName.attributeName -> this.firstname != null
            IdAustriaAttribute.LastName.attributeName -> this.lastname != null
            IdAustriaAttribute.DateOfBirth.attributeName -> this.dateOfBirth != null
            IdAustriaAttribute.Portrait.attributeName -> this.portrait != null
            IdAustriaAttribute.AgeAtLeast14.attributeName -> this.ageAtLeast14 == true
            IdAustriaAttribute.AgeAtLeast16.attributeName -> this.ageAtLeast16 == true
            IdAustriaAttribute.AgeAtLeast18.attributeName -> this.ageAtLeast18 == true
            IdAustriaAttribute.AgeAtLeast21.attributeName -> this.ageAtLeast21 == true
            IdAustriaAttribute.DrivingPermissions.attributeName -> this.drivingPermissions.isNotEmpty()
            IdAustriaAttribute.StreetName.attributeName -> this.streetName != null
            IdAustriaAttribute.PostalCode.attributeName -> this.postalCode != null
            IdAustriaAttribute.TownName.attributeName -> this.townName != null
            IdAustriaAttribute.CarModel.attributeName -> this.carModel != null
            IdAustriaAttribute.LicensePlateNumber.attributeName -> this.licensePlateNumber != null
            else -> TODO("Unsupported attribute: $attributeName")
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////
// firstname
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
                this.disclosures.filter { it.value?.claimName == IdAustriaAttribute.FirstName.attributeName }
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
// lastname
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
                this.disclosures.filter { it.value?.claimName == IdAustriaAttribute.LastName.attributeName }
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
// dateOfBirth
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
                this.disclosures.filter { it.value?.claimName == IdAustriaAttribute.DateOfBirth.attributeName }
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
// portrait
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
                null
//                TODO("Missing Implementation")
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
// ageAtLeast14
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
                TODO("Missing Implementation")
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
// ageAtLeast16
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
                TODO("Missing Implementation")
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
// ageAtLeast18
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
                TODO("Missing Implementation")
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
// ageAtLeast21
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
                TODO("Missing Implementation")
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
// drivingPermissions
//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.drivingPermissions: List<String>
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                listOf("Klasse A", "Klasse B")
//                TODO("Missing Implementation")
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.drivingPermissions: List<String>
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                listOf("Klasse A", "Klasse B")
//                TODO("Missing Implementation")
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
// streetName
//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.streetName: String?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                credentialSubject.mainAddress
//                TODO("Missing Implementation")
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.streetName: String?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                TODO("Missing Implementation")
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.streetName: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.streetName
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.streetName
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////
// postalCode
//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.postalCode: String?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                TODO("Missing Implementation")
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.postalCode: String?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                TODO("Missing Implementation")
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.postalCode: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.postalCode
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.postalCode
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////
// townName
//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.townName: String?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                TODO("Missing Implementation")
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.townName: String?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                TODO("Missing Implementation")
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.townName: String?
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.townName
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.townName
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////
// carModel
//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.carModel: String?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                TODO("Missing Implementation")
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.carModel: String?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                TODO("Missing Implementation")
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
// licensePlateNumber
//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.licensePlateNumber: String?
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                TODO("Missing Implementation")
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.licensePlateNumber: String?
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                TODO("Missing Implementation")
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
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
                this.disclosures.filter { it.value?.claimName == "firstname" }
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
                this.disclosures.filter { it.value?.claimName == "lastname" }
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
                this.disclosures.filter { it.value?.claimName == "date-of-birth" }
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
// ageLowerBounds
//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.ageLowerBounds: List<Int>
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                listOfNotNull(
                    credentialSubject.ageOver14?.let { if (it) 14 else null },
                    credentialSubject.ageOver16?.let { if (it) 16 else null },
                    credentialSubject.ageOver18?.let { if (it) 18 else null },
                    credentialSubject.ageOver21?.let { if (it) 21 else null },
                )
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.ageLowerBounds: List<Int>
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                listOf()
//                TODO("Missing Implementation")
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.ageLowerBounds: List<Int>
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.ageLowerBounds
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.ageLowerBounds
            }

            else -> TODO(invalidCredentialStoreEntry)
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////
// ageUpperBounds
//////////////////////////////////////////////////////////////////////////////////////////

val SubjectCredentialStore.StoreEntry.Vc.ageUpperBounds: List<Int>
    get() {
        return when (val credentialSubject = this.vc.vc.credentialSubject) {
            is IdAustriaCredential -> {
                listOfNotNull(
                    credentialSubject.ageOver14?.let { if(it) null else 14 },
                    credentialSubject.ageOver16?.let { if(it) null else 16 },
                    credentialSubject.ageOver18?.let { if(it) null else 18 },
                    credentialSubject.ageOver21?.let { if(it) null else 21 },
                )
            }

            else -> TODO(invalidCredentialSubjectMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.SdJwt.ageUpperBounds: List<Int>
    get() {
        return when (this.scheme) {
            is IdAustriaScheme -> {
                listOf()
//                TODO("Missing Implementation")
            }

            else -> TODO(invalidCredentialSchemeMessage)
        }
    }

val SubjectCredentialStore.StoreEntry.ageUpperBounds: List<Int>
    get() {
        return when (this) {
            is SubjectCredentialStore.StoreEntry.Vc -> {
                this.ageUpperBounds
            }

            is SubjectCredentialStore.StoreEntry.SdJwt -> {
                this.ageUpperBounds
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
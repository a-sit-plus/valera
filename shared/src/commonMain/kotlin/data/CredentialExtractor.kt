package data

import at.asitplus.wallet.eupid.EuPidCredential
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.mdl.DrivingPrivilege
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import io.github.aakira.napier.Napier
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.decodeBase64String
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

private val SubjectCredentialStore.StoreEntry.unsupportedCredentialStoreEntry: String
    get() = "Unsupported credential store entry: $this"

////////////////////////////////////////////////////////////////////////////////////////////////////

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
    fun containsAttribute(
        credentialScheme: ConstantIndex.CredentialScheme,
        attributeName: String
    ): Boolean {
        return when (credentialScheme) {
            is IdAustriaScheme -> when (attributeName) {
                IdAustriaScheme.Attributes.BPK -> this.bpk != null
                IdAustriaScheme.Attributes.FIRSTNAME -> this.givenName != null
                IdAustriaScheme.Attributes.LASTNAME -> this.familyName != null
                IdAustriaScheme.Attributes.DATE_OF_BIRTH -> this.dateOfBirth != null
                IdAustriaScheme.Attributes.PORTRAIT -> this.portrait != null
                IdAustriaScheme.Attributes.AGE_OVER_14 -> this.ageAtLeast14 != null
                IdAustriaScheme.Attributes.AGE_OVER_16 -> this.ageAtLeast16 != null
                IdAustriaScheme.Attributes.AGE_OVER_18 -> this.ageAtLeast18 != null
                IdAustriaScheme.Attributes.AGE_OVER_21 -> this.ageAtLeast21 != null
                IdAustriaScheme.Attributes.MAIN_ADDRESS -> this.idAustriaCredentialMainAddressAdapter != null
                else -> false
            }

            is EuPidScheme -> when (attributeName) {
                EuPidScheme.Attributes.GIVEN_NAME -> this.givenName != null
                EuPidScheme.Attributes.FAMILY_NAME -> this.familyName != null
                EuPidScheme.Attributes.BIRTH_DATE -> this.dateOfBirth != null
                EuPidScheme.Attributes.AGE_OVER_18 -> this.ageAtLeast18 != null
                EuPidScheme.Attributes.RESIDENT_ADDRESS -> this.mainResidenceAddress != null
                EuPidScheme.Attributes.RESIDENT_STREET -> this.mainResidenceStreetName != null
                EuPidScheme.Attributes.RESIDENT_CITY -> this.mainResidenceTownName != null
                EuPidScheme.Attributes.RESIDENT_POSTAL_CODE -> this.mainResidencePostalCode != null
                EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER -> this.mainResidenceHouseNumber != null
                EuPidScheme.Attributes.RESIDENT_COUNTRY -> this.mainResidenceCountryName != null
                EuPidScheme.Attributes.RESIDENT_STATE -> this.mainResidenceStateName != null
                EuPidScheme.Attributes.GENDER -> this.gender != null
                EuPidScheme.Attributes.NATIONALITY -> this.nationality != null
                EuPidScheme.Attributes.AGE_IN_YEARS -> this.ageInYears != null
                EuPidScheme.Attributes.AGE_BIRTH_YEAR -> this.yearOfBirth != null
                EuPidScheme.Attributes.FAMILY_NAME_BIRTH -> this.familyNameAtBirth != null
                EuPidScheme.Attributes.GIVEN_NAME_BIRTH -> this.givenNameAtBirth != null
                EuPidScheme.Attributes.BIRTH_PLACE -> this.birthPlace != null
                EuPidScheme.Attributes.BIRTH_COUNTRY -> this.birthCountry != null
                EuPidScheme.Attributes.BIRTH_STATE -> this.birthState != null
                EuPidScheme.Attributes.BIRTH_CITY -> this.birthCity != null
                else -> false
            }

            is MobileDrivingLicenceScheme -> when (attributeName) {
                MobileDrivingLicenceDataElements.GIVEN_NAME -> this.givenName != null
                MobileDrivingLicenceDataElements.FAMILY_NAME -> this.familyName != null
                MobileDrivingLicenceDataElements.BIRTH_DATE -> this.dateOfBirth != null
                MobileDrivingLicenceDataElements.AGE_OVER_18 -> this.ageAtLeast18 != null
                MobileDrivingLicenceDataElements.NATIONALITY -> this.nationality != null
                MobileDrivingLicenceDataElements.RESIDENT_ADDRESS -> this.mainResidenceAddress != null
                MobileDrivingLicenceDataElements.RESIDENT_CITY -> this.mainResidenceTownName != null
                MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE -> this.mainResidencePostalCode != null
                MobileDrivingLicenceDataElements.RESIDENT_COUNTRY -> this.mainResidenceCountryName != null
                MobileDrivingLicenceDataElements.RESIDENT_STATE -> this.mainResidenceStateName != null
                MobileDrivingLicenceDataElements.AGE_IN_YEARS -> this.ageInYears != null
                MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR -> this.yearOfBirth != null
                MobileDrivingLicenceDataElements.BIRTH_PLACE -> this.birthPlace != null
                MobileDrivingLicenceDataElements.PORTRAIT -> this.portrait != null
                MobileDrivingLicenceDataElements.DOCUMENT_NUMBER -> this.documentNumber != null
                MobileDrivingLicenceDataElements.ISSUING_AUTHORITY -> this.issuingAuthority != null
                MobileDrivingLicenceDataElements.ISSUE_DATE -> this.issueDate != null
                MobileDrivingLicenceDataElements.EXPIRY_DATE -> this.expiryDate != null
                MobileDrivingLicenceDataElements.ISSUING_COUNTRY -> this.issuingCountry != null
                MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES -> this.drivingPrivileges != null
                MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN -> this.distinguishingSign != null
                else -> false
            }

            else -> false
        }
    }

    val bpk: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.bpk

                        is EuPidCredential -> null

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.BPK }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }


                        is EuPidScheme -> null


                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.BPK
                        }?.value?.elementValue?.let { (it as String) }

                        is EuPidScheme -> null

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val givenName: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.firstname

                        is EuPidCredential -> credentialSubject.givenName

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter {
                            it.value?.claimName == IdAustriaScheme.Attributes.FIRSTNAME
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.GIVEN_NAME
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.GIVEN_NAME
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.FIRSTNAME
                        }?.value?.elementValue

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(
                            EuPidScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.GIVEN_NAME
                        }?.value?.elementValue

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.GIVEN_NAME
                        }?.value?.elementValue

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        } as String?

    val familyName: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.lastname

                        is EuPidCredential -> credentialSubject.familyName

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter {
                            it.value?.claimName == IdAustriaScheme.Attributes.LASTNAME
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.FAMILY_NAME
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.FAMILY_NAME
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.LASTNAME
                        }?.value?.elementValue

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.FAMILY_NAME
                        }?.value?.elementValue

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.FAMILY_NAME
                        }?.value?.elementValue

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        } as String?

    val dateOfBirth: LocalDate?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.dateOfBirth

                        is EuPidCredential -> credentialSubject.birthDate

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter {
                            it.value?.claimName == IdAustriaScheme.Attributes.DATE_OF_BIRTH
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.let { LocalDate.parse(it) }

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.BIRTH_DATE
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.let { LocalDate.parse(it) }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.BIRTH_DATE
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.let { LocalDate.parse(it) }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.DATE_OF_BIRTH
                        }?.value?.elementValue

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.BIRTH_DATE
                        }?.value?.elementValue

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.BIRTH_DATE
                        }?.value?.elementValue

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            } as LocalDate?
        }

    val portrait: ByteArray?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credentialSubject.portrait

                        is EuPidCredential -> null

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter {
                            it.value?.claimName == IdAustriaScheme.Attributes.PORTRAIT
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.decodeBase64Bytes()

                        is EuPidScheme -> null

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.PORTRAIT
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.decodeBase64Bytes()

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.PORTRAIT
                        }?.value?.elementValue?.let { (it as String).decodeBase64Bytes() }

                        is EuPidScheme -> null

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.PORTRAIT
                        }?.value?.elementValue?.let { (it as String).decodeBase64Bytes() }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val gender: IsoIec5218Gender?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.gender

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.GENDER }
                            .firstNotNullOfOrNull { it.value?.claimValue as IsoIec5218Gender }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> {
                            val value = credential.issuerSigned.namespaces?.get(
                                EuPidScheme.isoNamespace
                            )?.entries?.firstOrNull {
                                it.value.elementIdentifier == EuPidScheme.Attributes.GENDER
                            }?.value?.elementValue
                            Napier.d("gender value: $value")
                            null
                        }

                        else -> null
                    }
                }

                else -> null
            }
        }

    val nationality: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.nationality

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.NATIONALITY
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.NATIONALITY
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(
                            EuPidScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.NATIONALITY
                        }?.value?.elementValue?.let { (it as String)}

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.NATIONALITY
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
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

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_14 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }

                        is EuPidScheme -> null

                        is MobileDrivingLicenceScheme -> null

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.AGE_OVER_14
                        }?.value?.elementValue?.let { (it as Boolean) }

                        is EuPidScheme -> null

                        is MobileDrivingLicenceScheme -> null

                        else -> null
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

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_16 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }

                        is EuPidScheme -> null

                        is MobileDrivingLicenceScheme -> null

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.AGE_OVER_16
                        }?.value?.elementValue?.let { (it as Boolean) }

                        is EuPidScheme -> null

                        is MobileDrivingLicenceScheme -> null

                        else -> null
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

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter {
                            it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_18
                        }.firstNotNullOfOrNull { it.value?.claimValue as Boolean }

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.AGE_OVER_18
                        }.firstNotNullOfOrNull { it.value?.claimValue as Boolean }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.AGE_OVER_18
                        }.firstNotNullOfOrNull { it.value?.claimValue as Boolean }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.AGE_OVER_18
                        }?.value?.elementValue?.let { (it as Boolean) }

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(
                            EuPidScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.AGE_OVER_18
                        }?.value?.elementValue?.let { (it as Boolean) }

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.AGE_OVER_18
                        }?.value?.elementValue?.let { (it as Boolean) }

                        else -> null
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

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.disclosures.filter { it.value?.claimName == IdAustriaScheme.Attributes.AGE_OVER_21 }
                            .firstNotNullOfOrNull { it.value?.claimValue as Boolean }

                        is EuPidScheme -> null

                        is MobileDrivingLicenceScheme -> null

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.issuerSigned.namespaces?.get(
                            IdAustriaScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == IdAustriaScheme.Attributes.AGE_OVER_21
                        }?.value?.elementValue?.let { (it as Boolean) }

                        is EuPidScheme -> null

                        else -> null
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
                    }?.value?.elementValue?.let { (it as String) }

                    else -> null
                }
            }

            else -> null
        }?.decodeBase64String()?.let {
            jsonSerializer.decodeFromString<IdAustriaMainAddressAdapter>(it)
        }

    val mainResidenceAddress: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.residentAddress

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.RESIDENT_ADDRESS
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.RESIDENT_ADDRESS
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(
                            EuPidScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_ADDRESS
                        }?.value?.elementValue?.let { (it as String) }

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.RESIDENT_ADDRESS
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val mainResidenceStreetName: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> credential.idAustriaCredentialMainAddressAdapter?.Strasse

                        is EuPidCredential -> credentialSubject.residentStreet

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Strasse

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.RESIDENT_STREET
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        is MobileDrivingLicenceScheme -> null

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Strasse

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(
                            EuPidScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_STREET
                        }?.value?.elementValue?.let { (it as String) }

                        is MobileDrivingLicenceScheme -> null

                        else -> null
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

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Hausnummer

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        is MobileDrivingLicenceScheme -> null

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Hausnummer

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER
                        }?.value?.elementValue?.let { (it as String) }

                        is MobileDrivingLicenceScheme -> null

                        else -> null
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

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Stiege

                        is EuPidScheme -> null

                        is MobileDrivingLicenceScheme -> null

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Stiege

                        is EuPidScheme -> null

                        is MobileDrivingLicenceScheme -> null

                        else -> null
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

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Tuer

                        is EuPidScheme -> null

                        is MobileDrivingLicenceScheme -> null

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Tuer

                        is EuPidScheme -> null

                        is MobileDrivingLicenceScheme -> null

                        else -> null
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

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Ortschaft

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.RESIDENT_CITY
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.RESIDENT_CITY
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Ortschaft

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(
                            EuPidScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_CITY
                        }?.value?.elementValue?.let { (it as String) }

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.RESIDENT_CITY
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
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

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Postleitzahl

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.RESIDENT_POSTAL_CODE
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> credential.idAustriaCredentialMainAddressAdapter?.Postleitzahl

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(
                            EuPidScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_POSTAL_CODE
                        }?.value?.elementValue?.let { (it as String) }

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
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

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.RESIDENT_STATE
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.RESIDENT_STATE
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_STATE
                        }?.value?.elementValue?.let { (it as String) }

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.RESIDENT_STATE
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
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

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.RESIDENT_COUNTRY
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.RESIDENT_COUNTRY
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(
                            EuPidScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.RESIDENT_COUNTRY
                        }?.value?.elementValue?.let { (it as String) }

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.RESIDENT_COUNTRY
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val ageInYears: UInt?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.ageInYears

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.AGE_IN_YEARS
                        }.firstNotNullOfOrNull { it.value?.claimValue as UInt }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.AGE_IN_YEARS
                        }.firstNotNullOfOrNull { it.value?.claimValue as UInt }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(
                            EuPidScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.AGE_IN_YEARS
                        }?.value?.elementValue?.let { (it as String) }?.toUInt()

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.AGE_IN_YEARS
                        }?.value?.elementValue?.let { (it as String) }?.toUInt()

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val yearOfBirth: UInt?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.ageBirthYear

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.AGE_BIRTH_YEAR
                        }.firstNotNullOfOrNull { it.value?.claimValue as UInt }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR
                        }.firstNotNullOfOrNull { it.value?.claimValue as UInt }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(
                            EuPidScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.AGE_BIRTH_YEAR
                        }?.value?.elementValue?.let { (it as String) }?.toUInt()

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR
                        }?.value?.elementValue?.let { (it as String) }?.toUInt()

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val familyNameAtBirth: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.familyNameBirth

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.FAMILY_NAME_BIRTH }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.FAMILY_NAME_BIRTH
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val givenNameAtBirth: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.givenNameBirth

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.GIVEN_NAME_BIRTH }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.GIVEN_NAME_BIRTH
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val birthPlace: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.birthPlace

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter {
                            it.value?.claimName == EuPidScheme.Attributes.BIRTH_PLACE
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.BIRTH_PLACE
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(
                            EuPidScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.BIRTH_PLACE
                        }?.value?.elementValue?.let { (it as String) }

                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.BIRTH_PLACE
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val birthCountry: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.birthCountry

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.BIRTH_COUNTRY }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.BIRTH_COUNTRY
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val birthState: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.birthState

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.BIRTH_STATE }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.BIRTH_STATE
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val birthCity: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.birthCity

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.BIRTH_CITY }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.BIRTH_CITY
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val documentNumber: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> null

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.DOCUMENT_NUMBER
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.DOCUMENT_NUMBER
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val issuingAuthority: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> null

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.ISSUING_AUTHORITY
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.ISSUING_AUTHORITY
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val issuingCountry: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> null

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.ISSUING_COUNTRY
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.ISSUING_COUNTRY
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val distinguishingSign: String?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> null

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN
                        }?.value?.elementValue?.let { (it as String) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val drivingPrivileges: Array<DrivingPrivilege>?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> null

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES
                        }.firstNotNullOfOrNull {
                            kotlin.runCatching { it.value?.claimValue as Array<DrivingPrivilege>? }.getOrNull()
                                ?: kotlin.runCatching {
                                    it.value?.claimValue?.toString()
                                        ?.let { jsonSerializer.decodeFromString<Array<DrivingPrivilege>>(it) }
                                }.getOrNull()
                        }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> credential.issuerSigned.namespaces?.get(
                            MobileDrivingLicenceScheme.isoNamespace
                        )?.entries?.firstOrNull {
                            it.value.elementIdentifier == MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES
                        }?.value?.elementValue?.let { (it as Array<DrivingPrivilege>) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val issueDate: LocalDate?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> null

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.ISSUE_DATE
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.let { LocalDate.parse(it) }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> {
                            val elementValue = credential.issuerSigned.namespaces?.get(
                                MobileDrivingLicenceScheme.isoNamespace
                            )?.entries?.firstOrNull {
                                it.value.elementIdentifier == MobileDrivingLicenceDataElements.ISSUE_DATE
                            }?.value?.elementValue
                            elementValue?.let { it as LocalDate } ?: elementValue?.let { (it as String) }?.let { LocalDate.parse(it) }
                        }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val expiryDate: LocalDate?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> null

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> credential.disclosures.filter {
                            it.value?.claimName == MobileDrivingLicenceDataElements.EXPIRY_DATE
                        }.firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.let { LocalDate.parse(it) }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is MobileDrivingLicenceScheme -> {
                            val elementValue = credential.issuerSigned.namespaces?.get(
                                MobileDrivingLicenceScheme.isoNamespace
                            )?.entries?.firstOrNull {
                                it.value.elementIdentifier == MobileDrivingLicenceDataElements.EXPIRY_DATE
                            }?.value?.elementValue
                            elementValue?.let { it as LocalDate } ?: elementValue?.let { (it as String) }?.let { LocalDate.parse(it) }
                        }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }

    val birthDate: LocalDate?
        get() = credentials.firstNotNullOfOrNull { credential ->
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    when (val credentialSubject = credential.vc.vc.credentialSubject) {
                        is IdAustriaCredential -> null

                        is EuPidCredential -> credentialSubject.birthDate

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.disclosures.filter { it.value?.claimName == EuPidScheme.Attributes.BIRTH_PLACE }
                            .firstNotNullOfOrNull { it.value?.claimValue as String }
                            ?.let { LocalDate.parse(it) }

                        else -> null
                    }
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    when (credential.scheme) {
                        is IdAustriaScheme -> null

                        is EuPidScheme -> credential.issuerSigned.namespaces?.get(EuPidScheme.isoNamespace)?.entries?.firstOrNull {
                            it.value.elementIdentifier == EuPidScheme.Attributes.BIRTH_PLACE
                        }?.value?.elementValue?.let { (it as String) }?.let { LocalDate.parse(it) }

                        else -> null
                    }
                }

                else -> TODO(credential.unsupportedCredentialStoreEntry)
            }
        }
}
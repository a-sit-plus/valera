package data.credentials

import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.CredentialAdapter.Companion.toAttributeMap
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import kotlinx.datetime.LocalDate

//sealed interface MobileDrivingLicenceCredentialAdapter : CredentialAdapter {
//    val givenName: String
//    val familyName: String
//    val birthDate: LocalDate
//    val ageAtLeast18: Boolean?
//    val nationality: String?
//    val residentAddress: String?
//    val residentCity: String?
//    val residentPostalCode: String?
//    val residentCountry: String?
//    val residentState: String?
//    val ageInYears: String?
//    val ageBirthYear: String?
//    val birthPlace: String?
//    val portrait: ByteArray?
//    val documentNumber: ByteArray?
//    val issuingAuthority: ByteArray?
//    val issueDate: ByteArray?
//    val expiryDate: ByteArray?
//    val issuingCountry: ByteArray?
//    val drivingPrivileges: ByteArray?
//    val unDistinguishingSign: ByteArray?
//
//    companion object {
//        fun createFromStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry): MobileDrivingLicenceCredentialAdapter {
//            if (storeEntry.scheme !is IdAustriaScheme) {
//                throw IllegalArgumentException("credential")
//            }
//            return when (storeEntry) {
//                is SubjectCredentialStore.StoreEntry.Vc -> {
//                    throw IllegalArgumentException("credential")
//                }
//
//                is SubjectCredentialStore.StoreEntry.SdJwt -> {
//                    MobileDrivingLicenceCredentialSdJwtAdapter(
//                        storeEntry.toAttributeMap(),
//                    )
//                }
//
//                is SubjectCredentialStore.StoreEntry.Iso -> {
//                    MobileDrivingLicenceCredentialIsoMdocAdapter(
//                        storeEntry.toNamespaceAttributeMap(),
//                    )
//                }
//            }
//        }
//    }
//}
//
//private class MobileDrivingLicenceCredentialSdJwtAdapter(
//    val attributes: Map<String, Any>
//) : MobileDrivingLicenceCredentialAdapter {
//    override val givenName: String
//        get() = TODO("Not yet implemented")
//    override val familyName: String
//        get() = TODO("Not yet implemented")
//    override val birthDate: LocalDate
//        get() = TODO("Not yet implemented")
//    override val ageAtLeast18: Boolean?
//        get() = TODO("Not yet implemented")
//    override val nationality: String?
//        get() = TODO("Not yet implemented")
//    override val residentAddress: String?
//        get() = TODO("Not yet implemented")
//    override val residentCity: String?
//        get() = TODO("Not yet implemented")
//    override val residentPostalCode: String?
//        get() = TODO("Not yet implemented")
//    override val residentCountry: String?
//        get() = TODO("Not yet implemented")
//    override val residentState: String?
//        get() = TODO("Not yet implemented")
//    override val ageInYears: String?
//        get() = TODO("Not yet implemented")
//    override val ageBirthYear: String?
//        get() = TODO("Not yet implemented")
//    override val birthPlace: String?
//        get() = TODO("Not yet implemented")
//    override val portrait: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val documentNumber: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val issuingAuthority: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val issueDate: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val expiryDate: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val issuingCountry: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val drivingPrivileges: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val unDistinguishingSign: ByteArray?
//        get() = TODO("Not yet implemented")
//}
//
//private class MobileDrivingLicenceCredentialIsoMdocAdapter(
//    namespaces: Map<String, Map<String, Any>>?,
//) : MobileDrivingLicenceCredentialAdapter {
//    override val givenName: String
//        get() = TODO("Not yet implemented")
//    override val familyName: String
//        get() = TODO("Not yet implemented")
//    override val birthDate: LocalDate
//        get() = TODO("Not yet implemented")
//    override val ageAtLeast18: Boolean?
//        get() = TODO("Not yet implemented")
//    override val nationality: String?
//        get() = TODO("Not yet implemented")
//    override val residentAddress: String?
//        get() = TODO("Not yet implemented")
//    override val residentCity: String?
//        get() = TODO("Not yet implemented")
//    override val residentPostalCode: String?
//        get() = TODO("Not yet implemented")
//    override val residentCountry: String?
//        get() = TODO("Not yet implemented")
//    override val residentState: String?
//        get() = TODO("Not yet implemented")
//    override val ageInYears: String?
//        get() = TODO("Not yet implemented")
//    override val ageBirthYear: String?
//        get() = TODO("Not yet implemented")
//    override val birthPlace: String?
//        get() = TODO("Not yet implemented")
//    override val portrait: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val documentNumber: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val issuingAuthority: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val issueDate: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val expiryDate: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val issuingCountry: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val drivingPrivileges: ByteArray?
//        get() = TODO("Not yet implemented")
//    override val unDistinguishingSign: ByteArray?
//        get() = TODO("Not yet implemented")
//}
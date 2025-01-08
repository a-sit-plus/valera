package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.companyregistration.Address
import at.asitplus.wallet.companyregistration.Branch
import at.asitplus.wallet.companyregistration.CompanyActivity
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.BRANCH
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_ACTIVITY
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_CONTACT_DATA
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_END_DATE
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_EUID
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_NAME
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_STATUS
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.COMPANY_TYPE
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.POSTAL_ADDRESS
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.REGISTERED_ADDRESS
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.REGISTRATION_DATE
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements.VAT_NUMBER
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.companyregistration.ContactData
import at.asitplus.wallet.lib.agent.SubjectCredentialStore.StoreEntry
import data.Attribute
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

sealed class CompanyRegistrationCredentialAdapter : CredentialAdapter() {

    override fun getAttribute(path: NormalizedJsonPath) = path.segments.firstOrNull()?.let { first ->
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                COMPANY_NAME -> Attribute.fromValue(companyName)
                COMPANY_TYPE -> Attribute.fromValue(companyType)
                COMPANY_STATUS -> Attribute.fromValue(companyStatus)
                COMPANY_ACTIVITY -> null // TODO Attribute.fromValue(companyActivity)
                REGISTRATION_DATE -> Attribute.fromValue(registrationDate)
                COMPANY_END_DATE -> Attribute.fromValue(companyEndDate)
                COMPANY_EUID -> Attribute.fromValue(companyEuid)
                VAT_NUMBER -> Attribute.fromValue(vatNumber)
                COMPANY_CONTACT_DATA -> null // TODO Attribute.fromValue(contactData)
                REGISTERED_ADDRESS -> null // TODO Attribute.fromValue(registeredAddress)
                POSTAL_ADDRESS -> null // TODO Attribute.fromValue(postalAddress)
                BRANCH -> null // TODO Attribute.fromValue(branch)
                else -> null
            }

            else -> null
        }
    }

    abstract val companyName: String?
    abstract val companyType: String?
    abstract val companyStatus: String?
    abstract val companyActivity: CompanyActivity?
    abstract val registrationDate: LocalDate?
    abstract val companyEndDate: LocalDate?
    abstract val companyEuid: String?
    abstract val vatNumber: String?
    abstract val contactData: ContactData?
    abstract val registeredAddress: Address?
    abstract val postalAddress: Address?
    abstract val branch: Branch?

    companion object {
        fun createFromStoreEntry(storeEntry: StoreEntry): CompanyRegistrationCredentialAdapter {
            require(storeEntry.scheme is CompanyRegistrationScheme)
            return when (storeEntry) {
                is StoreEntry.Vc -> TODO("Operation not yet supported")
                is StoreEntry.SdJwt -> CompanyRegistrationCredentialSdJwtAdapter(storeEntry.toAttributeMap())
                is StoreEntry.Iso -> TODO("Operation not yet supported")
            }
        }
    }
}

private class CompanyRegistrationCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>,
) : CompanyRegistrationCredentialAdapter() {

    override val companyName: String?
        get() = attributes[COMPANY_NAME]?.contentOrNull

    override val companyType: String?
        get() = attributes[COMPANY_TYPE]?.contentOrNull

    override val companyStatus: String?
        get() = attributes[COMPANY_STATUS]?.contentOrNull

    override val companyActivity: CompanyActivity? = null
        //TODO get() = attributes[COMPANY_ACTIVITY]?.contentOrNull

    override val registrationDate: LocalDate?
        get() = attributes[REGISTRATION_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val companyEndDate: LocalDate?
        get() = attributes[COMPANY_END_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val companyEuid: String?
        get() = attributes[COMPANY_EUID]?.contentOrNull

    override val vatNumber: String?
        get() = attributes[VAT_NUMBER]?.contentOrNull

    override val contactData: ContactData? = null
        // TODO get() = attributes[COMPANY_CONTACT_DATA]?.contentOrNull=

    override val registeredAddress: Address? = null
        // TODO get() = attributes[REGISTERED_ADDRESS]?.contentOrNull

    override val postalAddress: Address? = null
        // TODO get() = attributes[POSTAL_ADDRESS]?.contentOrNull

    override val branch: Branch? = null
        // TODO get() = attributes[BRANCH]?.contentOrNull
}

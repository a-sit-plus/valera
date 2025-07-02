package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
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
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.vckJsonSerializer
import data.Attribute
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

sealed class CompanyRegistrationCredentialAdapter : CredentialAdapter() {
    private fun CompanyRegistrationCredentialClaimDefinition.toAttribute() = when (this) {
        CompanyRegistrationCredentialClaimDefinition.COMPANY_NAME -> Attribute.fromValue(companyName)
        CompanyRegistrationCredentialClaimDefinition.COMPANY_TYPE -> Attribute.fromValue(companyType)
        CompanyRegistrationCredentialClaimDefinition.COMPANY_STATUS -> Attribute.fromValue(companyStatus)
        CompanyRegistrationCredentialClaimDefinition.COMPANY_ACTIVITY -> Attribute.fromValue(companyActivity)
        CompanyRegistrationCredentialClaimDefinition.REGISTRATION_DATE -> Attribute.fromValue(registrationDate)
        CompanyRegistrationCredentialClaimDefinition.COMPANY_END_DATE -> Attribute.fromValue(companyEndDate)
        CompanyRegistrationCredentialClaimDefinition.COMPANY_EUID -> Attribute.fromValue(companyEuid)
        CompanyRegistrationCredentialClaimDefinition.VAT_NUMBER -> Attribute.fromValue(vatNumber)
        CompanyRegistrationCredentialClaimDefinition.COMPANY_CONTACT_DATA -> Attribute.fromValue(contactData)
        CompanyRegistrationCredentialClaimDefinition.REGISTERED_ADDRESS -> Attribute.fromValue(registeredAddress)
        CompanyRegistrationCredentialClaimDefinition.POSTAL_ADDRESS -> Attribute.fromValue(postalAddress)
        CompanyRegistrationCredentialClaimDefinition.BRANCH -> Attribute.fromValue(branch)
    }

    fun getAttribute(claimDefinition: CompanyRegistrationCredentialClaimDefinition) = claimDefinition.toAttribute()

    override fun getAttribute(
        path: NormalizedJsonPath,
    ) = CompanyRegistrationCredentialClaimDefinitionResolver().resolveOrNull(
        SdJwtClaimReference(path)
    )?.toAttribute()

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
                is StoreEntry.SdJwt -> storeEntry.toComplexJson()
                    ?.let { CompanyRegistrationCredentialComplexSdJwtAdapter(it) }
                    ?: CompanyRegistrationCredentialSdJwtAdapter(storeEntry.toAttributeMap())

                is StoreEntry.Iso -> TODO("Operation not yet supported")
            }
        }
    }
}

private class CompanyRegistrationCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>,
) : CompanyRegistrationCredentialAdapter() {
    override val scheme: ConstantIndex.CredentialScheme
        get() = CompanyRegistrationScheme

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val companyName: String?
        get() = attributes[COMPANY_NAME]?.contentOrNull

    override val companyType: String?
        get() = attributes[COMPANY_TYPE]?.contentOrNull

    override val companyStatus: String?
        get() = attributes[COMPANY_STATUS]?.contentOrNull

    override val companyActivity: CompanyActivity?
        get() = attributes[COMPANY_ACTIVITY]?.contentOrNull?.let { vckJsonSerializer.decodeFromString(it) }

    override val registrationDate: LocalDate?
        get() = attributes[REGISTRATION_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val companyEndDate: LocalDate?
        get() = attributes[COMPANY_END_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val companyEuid: String?
        get() = attributes[COMPANY_EUID]?.contentOrNull

    override val vatNumber: String?
        get() = attributes[VAT_NUMBER]?.contentOrNull

    override val contactData: ContactData?
        get() = attributes[COMPANY_CONTACT_DATA]?.contentOrNull?.let { vckJsonSerializer.decodeFromString(it) }

    override val registeredAddress: Address?
        get() = attributes[REGISTERED_ADDRESS]?.contentOrNull?.let { vckJsonSerializer.decodeFromString(it) }

    override val postalAddress: Address?
        get() = attributes[POSTAL_ADDRESS]?.contentOrNull?.let { vckJsonSerializer.decodeFromString(it) }

    override val branch: Branch?
        get() = attributes[BRANCH]?.contentOrNull?.let { vckJsonSerializer.decodeFromString(it) }
}

private class CompanyRegistrationCredentialComplexSdJwtAdapter(
    private val attributes: JsonObject,
) : CompanyRegistrationCredentialAdapter() {
    override val scheme: ConstantIndex.CredentialScheme
        get() = CompanyRegistrationScheme

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val companyName: String?
        get() = (attributes[COMPANY_NAME] as? JsonPrimitive?)?.contentOrNull

    override val companyType: String?
        get() = (attributes[COMPANY_TYPE] as? JsonPrimitive?)?.contentOrNull

    override val companyStatus: String?
        get() = (attributes[COMPANY_STATUS] as? JsonPrimitive?)?.contentOrNull

    override val companyActivity: CompanyActivity?
        get() = (attributes[COMPANY_ACTIVITY] as? JsonObject?)?.let { vckJsonSerializer.decodeFromJsonElement(it) }

    override val registrationDate: LocalDate?
        get() = (attributes[REGISTRATION_DATE] as? JsonPrimitive?)?.contentOrNull?.toLocalDateOrNull()

    override val companyEndDate: LocalDate?
        get() = (attributes[COMPANY_END_DATE] as? JsonPrimitive?)?.contentOrNull?.toLocalDateOrNull()

    override val companyEuid: String?
        get() = (attributes[COMPANY_EUID] as? JsonPrimitive?)?.contentOrNull

    override val vatNumber: String?
        get() = (attributes[VAT_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val contactData: ContactData?
        get() = (attributes[COMPANY_CONTACT_DATA] as? JsonObject?)?.let { vckJsonSerializer.decodeFromJsonElement(it) }

    override val registeredAddress: Address?
        get() = (attributes[REGISTERED_ADDRESS] as? JsonObject?)?.let { vckJsonSerializer.decodeFromJsonElement(it) }

    override val postalAddress: Address?
        get() = (attributes[POSTAL_ADDRESS] as? JsonObject?)?.let { vckJsonSerializer.decodeFromJsonElement(it) }

    override val branch: Branch?
        get() = (attributes[BRANCH] as? JsonObject?)?.let { vckJsonSerializer.decodeFromJsonElement(it) }
}


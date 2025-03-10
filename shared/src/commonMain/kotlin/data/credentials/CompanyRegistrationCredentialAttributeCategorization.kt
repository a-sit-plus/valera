package data.credentialsdatacard

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.jsonpath.core.plus
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
import at.asitplus.wallet.por.PowerOfRepresentationDataElements
import data.PersonalDataCategory
import data.credentials.CredentialAttributeCategorization

object CompanyRegistrationCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.CompanyData to listOf(
            COMPANY_NAME,
            COMPANY_TYPE,
            COMPANY_STATUS,
            COMPANY_ACTIVITY,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.Metadata to listOf(
            REGISTRATION_DATE,
            COMPANY_END_DATE,
            COMPANY_EUID,
            VAT_NUMBER,
            COMPANY_CONTACT_DATA,
            REGISTERED_ADDRESS,
            POSTAL_ADDRESS,
            BRANCH,
        ).map { NormalizedJsonPath() + it to null },
    ),
    allAttributes = PowerOfRepresentationDataElements.ALL_ELEMENTS.map {
        NormalizedJsonPath() + it
    },
)
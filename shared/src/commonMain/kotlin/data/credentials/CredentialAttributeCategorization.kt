package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import data.PersonalDataCategory
import data.credentialsdatacard.CompanyRegistrationCredentialAttributeCategorization

/**
 * The type, `List<AttributeUnpackingInformation>`, is there to
 *  separate between the attribute to be requested (which is the first entry in the pair)
 *  and the subfields (e.g. if the field itself is an encoded json object) to be displayed in the generic data view (if any, otherwise null)
 */
typealias AttributeUnpackingInformation = Pair<NormalizedJsonPath, List<NormalizedJsonPath>?>

interface CredentialAttributeCategorization {
    companion object {
        fun load(
            scheme: ConstantIndex.CredentialScheme?,
            representation: ConstantIndex.CredentialRepresentation
        ): Template = when (scheme) {
            is IdAustriaScheme -> IdAustriaCredentialAttributeCategorization
            is EuPidScheme -> // TODO When mapping is confirmed if (representation == ConstantIndex.CredentialRepresentation.SD_JWT) EuPidCredentialSdJwtAttributeCategorization else EuPidCredentialAttributeCategorization
                EuPidCredentialAttributeCategorization
            is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialAttributeCategorization
            is PowerOfRepresentationScheme -> PowerOfRepresentationCredentialAttributeCategorization
            is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialAttributeCategorization
            is CompanyRegistrationScheme -> CompanyRegistrationCredentialAttributeCategorization
            is HealthIdScheme -> HealthIdCredentialAttributeCategorization
            is TaxIdScheme -> TaxIdCredentialAttributeCategorization
            else -> EmptyCredentialAttributeCategorization
        }
    }

    operator fun get(personalDataCategory: PersonalDataCategory): List<AttributeUnpackingInformation>?

    abstract class Template(
        categorization: Map<PersonalDataCategory, List<AttributeUnpackingInformation>>,
        allAttributes: Collection<NormalizedJsonPath>,
    ) : CredentialAttributeCategorization {
        private val categorization by lazy {
            categorization.withOthersFrom(allAttributes)
        }

        override operator fun get(personalDataCategory: PersonalDataCategory): List<AttributeUnpackingInformation>? {
            return categorization[personalDataCategory]
        }

        private fun Map<PersonalDataCategory, List<Pair<NormalizedJsonPath, List<NormalizedJsonPath>?>>>.withOthersFrom(
            allAttributes: Collection<NormalizedJsonPath>
        ): Map<PersonalDataCategory, List<Pair<NormalizedJsonPath, List<NormalizedJsonPath>?>>> {
            val categorization = this
            val categorizedAttributes =
                categorization.map { it.value.map { it.first } }.flatten()
            val otherAttributes = allAttributes.filterNot { uncategorized ->
                categorizedAttributes.any { categorized ->
                    categorized.toString() == uncategorized.toString()
                }
            }.map {
                NormalizedJsonPath() + it to (null as List<NormalizedJsonPath>?)
            }
            return categorization + (if (otherAttributes.isEmpty()) listOf() else listOf(
                PersonalDataCategory.OtherData to otherAttributes,
            ))
        }
    }
}

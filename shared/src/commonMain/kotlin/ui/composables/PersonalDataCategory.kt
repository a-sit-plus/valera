package ui.composables

import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.section_heading_admission_data
import composewalletapp.shared.generated.resources.section_heading_admission_data_icon_text
import composewalletapp.shared.generated.resources.section_heading_age_data
import composewalletapp.shared.generated.resources.section_heading_age_data_icon_text
import composewalletapp.shared.generated.resources.section_heading_driving_permission_data
import composewalletapp.shared.generated.resources.section_heading_driving_permission_data_icon_text
import composewalletapp.shared.generated.resources.section_heading_identity_data
import composewalletapp.shared.generated.resources.section_heading_identity_data_icon_text
import composewalletapp.shared.generated.resources.section_heading_other_data_category_icon_text
import composewalletapp.shared.generated.resources.section_heading_other_data_category_title
import composewalletapp.shared.generated.resources.section_heading_residence_data
import composewalletapp.shared.generated.resources.section_heading_residence_data_icon_text
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource

@OptIn(ExperimentalResourceApi::class)
enum class PersonalDataCategory(
    val iconText: StringResource,
    val categoryTitle: StringResource,
    val attributes: Map<ConstantIndex.CredentialScheme, Collection<String>> = mapOf(),
) {
    IdentityData(
        iconText = Res.string.section_heading_identity_data_icon_text,
        categoryTitle = Res.string.section_heading_identity_data,
        attributes = mapOf(
            IdAustriaScheme to listOf(
                IdAustriaScheme.Attributes.FIRSTNAME,
                IdAustriaScheme.Attributes.LASTNAME,
                IdAustriaScheme.Attributes.DATE_OF_BIRTH,
                IdAustriaScheme.Attributes.PORTRAIT,
            ),
            EuPidScheme to listOf(
                EuPidScheme.Attributes.GIVEN_NAME,
                EuPidScheme.Attributes.GIVEN_NAME_BIRTH,
                EuPidScheme.Attributes.FAMILY_NAME,
                EuPidScheme.Attributes.FAMILY_NAME_BIRTH,
                EuPidScheme.Attributes.BIRTH_DATE,
                EuPidScheme.Attributes.BIRTH_PLACE,
                EuPidScheme.Attributes.BIRTH_CITY,
                EuPidScheme.Attributes.BIRTH_COUNTRY,
                EuPidScheme.Attributes.BIRTH_STATE,
                EuPidScheme.Attributes.AGE_BIRTH_YEAR,
                EuPidScheme.Attributes.AGE_IN_YEARS,
                EuPidScheme.Attributes.GENDER,
                EuPidScheme.Attributes.NATIONALITY,
            ),
            MobileDrivingLicenceScheme to listOf(
                MobileDrivingLicenceDataElements.GIVEN_NAME,
                MobileDrivingLicenceDataElements.FAMILY_NAME,
                MobileDrivingLicenceDataElements.BIRTH_DATE,
                MobileDrivingLicenceDataElements.BIRTH_PLACE,
                MobileDrivingLicenceDataElements.NATIONALITY,
                MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR,
                MobileDrivingLicenceDataElements.AGE_IN_YEARS,
                MobileDrivingLicenceDataElements.PORTRAIT,
            ),
        ),
    ),
    AgeData(
        iconText = Res.string.section_heading_age_data_icon_text,
        categoryTitle = Res.string.section_heading_age_data,
        attributes = mapOf(
            IdAustriaScheme to listOf(
                IdAustriaScheme.Attributes.AGE_OVER_14,
                IdAustriaScheme.Attributes.AGE_OVER_16,
                IdAustriaScheme.Attributes.AGE_OVER_18,
                IdAustriaScheme.Attributes.AGE_OVER_21,
            ),
            EuPidScheme to listOf(
                EuPidScheme.Attributes.AGE_OVER_18,
            ),
            MobileDrivingLicenceScheme to listOf(
                MobileDrivingLicenceDataElements.AGE_OVER_18,
            ),
        ),
    ),
    ResidenceData(
        iconText = Res.string.section_heading_residence_data_icon_text,
        categoryTitle = Res.string.section_heading_residence_data,
        attributes = mapOf(
            IdAustriaScheme to listOf(
                IdAustriaScheme.Attributes.MAIN_ADDRESS,
            ),
            EuPidScheme to listOf(
                EuPidScheme.Attributes.RESIDENT_ADDRESS,
                EuPidScheme.Attributes.RESIDENT_COUNTRY,
                EuPidScheme.Attributes.RESIDENT_STATE,
                EuPidScheme.Attributes.RESIDENT_CITY,
                EuPidScheme.Attributes.RESIDENT_POSTAL_CODE,
                EuPidScheme.Attributes.RESIDENT_STREET,
                EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER,
            ),
            MobileDrivingLicenceScheme to listOf(
                MobileDrivingLicenceDataElements.RESIDENT_ADDRESS,
                MobileDrivingLicenceDataElements.RESIDENT_CITY,
                MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE,
                MobileDrivingLicenceDataElements.RESIDENT_COUNTRY,
                MobileDrivingLicenceDataElements.RESIDENT_STATE,
            ),
        ),
    ),
    DrivingPermissions(
        iconText = Res.string.section_heading_driving_permission_data_icon_text,
        categoryTitle = Res.string.section_heading_driving_permission_data
    ),
    AdmissionData(
        iconText = Res.string.section_heading_admission_data_icon_text,
        categoryTitle = Res.string.section_heading_admission_data
    ),
    OtherData(
        iconText = Res.string.section_heading_other_data_category_icon_text,
        categoryTitle = Res.string.section_heading_other_data_category_title
    ),
}

val attributeCategorizationOrder = listOf(
    PersonalDataCategory.IdentityData,
    PersonalDataCategory.ResidenceData,
    PersonalDataCategory.AgeData,
    PersonalDataCategory.DrivingPermissions,
    PersonalDataCategory.AdmissionData,
    PersonalDataCategory.OtherData,
)
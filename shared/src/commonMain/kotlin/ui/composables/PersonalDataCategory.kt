package ui.composables

import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
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
    val attributes: Collection<String> = listOf(),
) {
    IdentityData(
        iconText = Res.string.section_heading_identity_data_icon_text,
        categoryTitle = Res.string.section_heading_identity_data,
        attributes = listOf(
            IdAustriaScheme.Attributes.FIRSTNAME,
            EuPidScheme.Attributes.GIVEN_NAME,
            EuPidScheme.Attributes.GIVEN_NAME_BIRTH,
            IdAustriaScheme.Attributes.LASTNAME,
            EuPidScheme.Attributes.FAMILY_NAME,
            EuPidScheme.Attributes.FAMILY_NAME_BIRTH,
            IdAustriaScheme.Attributes.DATE_OF_BIRTH,
            EuPidScheme.Attributes.BIRTH_DATE,
            EuPidScheme.Attributes.BIRTH_PLACE,
            EuPidScheme.Attributes.BIRTH_CITY,
            EuPidScheme.Attributes.BIRTH_COUNTRY,
            EuPidScheme.Attributes.BIRTH_STATE,
            EuPidScheme.Attributes.AGE_BIRTH_YEAR,
            EuPidScheme.Attributes.AGE_IN_YEARS,
            IdAustriaScheme.Attributes.PORTRAIT,
            EuPidScheme.Attributes.GENDER,
            EuPidScheme.Attributes.NATIONALITY,
        ),
    ),
    AgeData(
        iconText = Res.string.section_heading_age_data_icon_text,
        categoryTitle = Res.string.section_heading_age_data,
        attributes = listOf(
            IdAustriaScheme.Attributes.AGE_OVER_14,
            IdAustriaScheme.Attributes.AGE_OVER_16,
            IdAustriaScheme.Attributes.AGE_OVER_18,
            EuPidScheme.Attributes.AGE_OVER_18,
            IdAustriaScheme.Attributes.AGE_OVER_21,
        ),
    ),
    ResidenceData(
        iconText = Res.string.section_heading_residence_data_icon_text,
        categoryTitle = Res.string.section_heading_residence_data,
        attributes = listOf(
            IdAustriaScheme.Attributes.MAIN_ADDRESS,
            EuPidScheme.Attributes.RESIDENT_ADDRESS,
            EuPidScheme.Attributes.RESIDENT_COUNTRY,
            EuPidScheme.Attributes.RESIDENT_STATE,
            EuPidScheme.Attributes.RESIDENT_CITY,
            EuPidScheme.Attributes.RESIDENT_POSTAL_CODE,
            EuPidScheme.Attributes.RESIDENT_STREET,
            EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER,
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
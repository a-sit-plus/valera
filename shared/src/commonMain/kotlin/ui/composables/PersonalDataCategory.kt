package ui.composables

import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.section_heading_admission_data
import composewalletapp.shared.generated.resources.section_heading_admission_data_icon_text
import composewalletapp.shared.generated.resources.section_heading_age_data
import composewalletapp.shared.generated.resources.section_heading_age_data_icon_text
import composewalletapp.shared.generated.resources.section_heading_driving_permission_data
import composewalletapp.shared.generated.resources.section_heading_driving_permission_data_icon_text
import composewalletapp.shared.generated.resources.section_heading_identity_data
import composewalletapp.shared.generated.resources.section_heading_identity_data_icon_text
import composewalletapp.shared.generated.resources.section_heading_residence_data
import composewalletapp.shared.generated.resources.section_heading_residence_data_icon_text
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource

@OptIn(ExperimentalResourceApi::class)
enum class PersonalDataCategory(
    val iconText: StringResource,
    val categoryTitle: StringResource,
) {
    IdentityData(iconText = Res.string.section_heading_identity_data_icon_text, categoryTitle = Res.string.section_heading_identity_data),
    AgeData(iconText = Res.string.section_heading_age_data_icon_text, categoryTitle = Res.string.section_heading_age_data),
    ResidenceData(iconText = Res.string.section_heading_residence_data_icon_text, categoryTitle = Res.string.section_heading_residence_data),
    DrivingPermissions(iconText = Res.string.section_heading_driving_permission_data_icon_text, categoryTitle = Res.string.section_heading_driving_permission_data),
    AdmissionData(iconText = Res.string.section_heading_admission_data_icon_text, categoryTitle = Res.string.section_heading_admission_data),
}
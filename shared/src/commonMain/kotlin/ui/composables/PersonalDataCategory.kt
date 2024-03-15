package ui.composables

import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.SECTION_HEADING_ADMISSION_DATA
import composewalletapp.shared.generated.resources.SECTION_HEADING_ADMISSION_DATA_ICON_TEXT
import composewalletapp.shared.generated.resources.SECTION_HEADING_AGE_DATA
import composewalletapp.shared.generated.resources.SECTION_HEADING_AGE_DATA_ICON_TEXT
import composewalletapp.shared.generated.resources.SECTION_HEADING_DRIVING_PERMISSION_DATA
import composewalletapp.shared.generated.resources.SECTION_HEADING_DRIVING_PERMISSION_DATA_ICON_TEXT
import composewalletapp.shared.generated.resources.SECTION_HEADING_IDENTITY_DATA
import composewalletapp.shared.generated.resources.SECTION_HEADING_IDENTITY_DATA_ICON_TEXT
import composewalletapp.shared.generated.resources.SECTION_HEADING_RESIDENCE_DATA
import composewalletapp.shared.generated.resources.SECTION_HEADING_RESIDENCE_DATA_ICON_TEXT
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource

@OptIn(ExperimentalResourceApi::class)
enum class PersonalDataCategory(
    val iconText: StringResource,
    val categoryTitle: StringResource,
) {
    IdentityData(iconText = Res.string.SECTION_HEADING_IDENTITY_DATA_ICON_TEXT, categoryTitle = Res.string.SECTION_HEADING_IDENTITY_DATA),
    AgeData(iconText = Res.string.SECTION_HEADING_AGE_DATA_ICON_TEXT, categoryTitle = Res.string.SECTION_HEADING_AGE_DATA),
    ResidenceData(iconText = Res.string.SECTION_HEADING_RESIDENCE_DATA_ICON_TEXT, categoryTitle = Res.string.SECTION_HEADING_RESIDENCE_DATA),
    DrivingPermissions(iconText = Res.string.SECTION_HEADING_DRIVING_PERMISSION_DATA_ICON_TEXT, categoryTitle = Res.string.SECTION_HEADING_DRIVING_PERMISSION_DATA),
    AdmissionData(iconText = Res.string.SECTION_HEADING_ADMISSION_DATA_ICON_TEXT, categoryTitle = Res.string.SECTION_HEADING_ADMISSION_DATA),
}
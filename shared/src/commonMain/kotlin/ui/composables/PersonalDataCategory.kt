package ui.composables

import Resources

enum class PersonalDataCategory(
    val iconText: String,
    val categoryTitle: String,
) {
    IdentityData(iconText = Resources.SECTION_HEADING_IDENTITY_DATA_ICON_TEXT, categoryTitle = Resources.SECTION_HEADING_IDENTITY_DATA),
    AgeData(iconText = Resources.SECTION_HEADING_AGE_DATA_ICON_TEXT, categoryTitle = Resources.SECTION_HEADING_AGE_DATA),
    ResidenceData(iconText = Resources.SECTION_HEADING_RESIDENCE_DATA_ICON_TEXT, categoryTitle = Resources.SECTION_HEADING_RESIDENCE_DATA),
    DrivingPermissions(iconText = Resources.SECTION_HEADING_DRIVING_PERMISSION_DATA_ICON_TEXT, categoryTitle = Resources.SECTION_HEADING_DRIVING_PERMISSION_DATA),
    AdmissionData(iconText = Resources.SECTION_HEADING_ADMISSION_DATA_ICON_TEXT, categoryTitle = Resources.SECTION_HEADING_ADMISSION_DATA),
}
package ui.composables

enum class PersonalDataCategory(
    val avatarText: String,
    val categoryName: String,
) {
    IdentityData(avatarText = "ID", categoryName = "Identitätsdaten"),
    AgeData(avatarText = "AS", categoryName = "Altersstufen"),
    ResidenceData(avatarText = "MA", categoryName = "Meldeadresse"),
    DrivingPermissions(avatarText = "LB", categoryName = "Lenkberechtigung"),
    AdmissionData(avatarText = "Z", categoryName = "Zulassungsdaten"),
}
package ui.composables

enum class PersonalDataCategory(
    val avatarText: String,
    val categoryName: String,
) {
    IdentityData(avatarText = "ID", categoryName = "Identitätsdaten"),
    AgeData(avatarText = "AS", categoryName = "Altersstufen"),
    ResidencyData(avatarText = "MA", categoryName = "Meldeadresse"),
    DrivingLicenseData(avatarText = "LB", categoryName = "Lenkberechtigung"),
    CarData(avatarText = "Z", categoryName = "Zulassungsdaten"),
}
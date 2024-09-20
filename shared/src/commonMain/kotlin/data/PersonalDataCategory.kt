package data

import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.section_heading_admission_data
import compose_wallet_app.shared.generated.resources.section_heading_admission_data_icon_text
import compose_wallet_app.shared.generated.resources.section_heading_age_data
import compose_wallet_app.shared.generated.resources.section_heading_age_data_icon_text
import compose_wallet_app.shared.generated.resources.section_heading_appearance_data
import compose_wallet_app.shared.generated.resources.section_heading_appearance_data_icon_text
import compose_wallet_app.shared.generated.resources.section_heading_biometric_data
import compose_wallet_app.shared.generated.resources.section_heading_biometric_data_icon_text
import compose_wallet_app.shared.generated.resources.section_heading_birth_data
import compose_wallet_app.shared.generated.resources.section_heading_birth_data_icon_text
import compose_wallet_app.shared.generated.resources.section_heading_driving_permission_data
import compose_wallet_app.shared.generated.resources.section_heading_driving_permission_data_icon_text
import compose_wallet_app.shared.generated.resources.section_heading_identity_data
import compose_wallet_app.shared.generated.resources.section_heading_identity_data_icon_text
import compose_wallet_app.shared.generated.resources.section_heading_metadata
import compose_wallet_app.shared.generated.resources.section_heading_metadata_icon_text
import compose_wallet_app.shared.generated.resources.section_heading_other_data_category_icon_text
import compose_wallet_app.shared.generated.resources.section_heading_other_data_category_title
import compose_wallet_app.shared.generated.resources.section_heading_representation_data
import compose_wallet_app.shared.generated.resources.section_heading_representation_data_icon_text
import compose_wallet_app.shared.generated.resources.section_heading_residence_data
import compose_wallet_app.shared.generated.resources.section_heading_residence_data_icon_text
import org.jetbrains.compose.resources.StringResource

enum class PersonalDataCategory(
    val iconText: StringResource,
    val categoryTitle: StringResource,
) {
    IdentityData(
        iconText = Res.string.section_heading_identity_data_icon_text,
        categoryTitle = Res.string.section_heading_identity_data,
    ),
    BirthData(
        iconText = Res.string.section_heading_birth_data_icon_text,
        categoryTitle = Res.string.section_heading_birth_data,
    ),
    AppearanceData(
        iconText = Res.string.section_heading_appearance_data_icon_text,
        categoryTitle = Res.string.section_heading_appearance_data,
    ),
    BiometricData(
        iconText = Res.string.section_heading_biometric_data_icon_text,
        categoryTitle = Res.string.section_heading_biometric_data,
    ),
    RepresentationData(
        iconText = Res.string.section_heading_representation_data_icon_text,
        categoryTitle = Res.string.section_heading_representation_data,
    ),
    AgeData(
        iconText = Res.string.section_heading_age_data_icon_text,
        categoryTitle = Res.string.section_heading_age_data,
    ),
    ResidenceData(
        iconText = Res.string.section_heading_residence_data_icon_text,
        categoryTitle = Res.string.section_heading_residence_data,
    ),
    DrivingPermissions(
        iconText = Res.string.section_heading_driving_permission_data_icon_text,
        categoryTitle = Res.string.section_heading_driving_permission_data
    ),
    AdmissionData(
        iconText = Res.string.section_heading_admission_data_icon_text,
        categoryTitle = Res.string.section_heading_admission_data
    ),
    Metadata(
        iconText = Res.string.section_heading_metadata_icon_text,
        categoryTitle = Res.string.section_heading_metadata
    ),
    OtherData(
        iconText = Res.string.section_heading_other_data_category_icon_text,
        categoryTitle = Res.string.section_heading_other_data_category_title
    ),
}
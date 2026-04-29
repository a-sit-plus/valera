package ui.presentation

data class DCQLCredentialQueryUiModel(
    val credentialRepresentationLocalized: String?,
    val credentialSchemeLocalized: String,
    val requestedAttributesLocalized: DCQLCredentialQueryUiModelAttributeLabels?,
)


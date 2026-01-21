package ui.presentation

data class DCQLCredentialQueryUiModel(
    val credentialRepresentationLocalized: String?,
    val credentialSchemeLocalized: String,
    val requestedAttributesLocalized: DCQLCredentialQueryUiModelAttributeLabels?,
)

data class DCQLCredentialQueryUiModelAttributeLabels(
    val attributesLocalized: List<String>,
    val otherAttributes: Int,
)
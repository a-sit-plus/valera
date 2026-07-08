package ui.presentation

data class DCQLCredentialQueryUiModelAttributeLabels(
    val attributesLocalized: List<String>,
    val otherAttributes: Int,
    val allowedAttributes: Map<String, Boolean>? = null
)
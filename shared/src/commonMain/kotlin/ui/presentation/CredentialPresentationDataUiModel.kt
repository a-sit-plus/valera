package ui.presentation

import kotlinx.serialization.json.JsonElement

data class CredentialPresentationDataUiModel(
    val credentialType: String,
    val credentialFormat: String,
    val disclosures: JsonElement?,
)
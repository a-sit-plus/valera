package ui.presentation

data class CredentialSetQueryOptionUiModel(
    val isSatisfiable: Boolean,
    val credentialQueries: List<DCQLCredentialQueryUiModel>
)
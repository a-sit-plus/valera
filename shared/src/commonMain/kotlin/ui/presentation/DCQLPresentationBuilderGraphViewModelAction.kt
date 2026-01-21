package ui.presentation

import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier

sealed interface DCQLPresentationBuilderGraphViewModelAction {
    data class SelectRequiredCredentialSetQueryOption(
        val credentialSetQueryIndex: UInt,
        val credentialSetQueryOptionIndex: UInt,
    ) : DCQLPresentationBuilderGraphViewModelAction

    data class SelectOptionalCredentialSetQueryOption(
        val credentialSetQueryIndex: UInt,
        val credentialSetQueryOptionIndex: UInt?,
    ) : DCQLPresentationBuilderGraphViewModelAction

    data class SelectSubmissions(
        val queryIdentifier: DCQLCredentialQueryIdentifier,
        val submissionIndices: List<UInt>,
    ) : DCQLPresentationBuilderGraphViewModelAction
}
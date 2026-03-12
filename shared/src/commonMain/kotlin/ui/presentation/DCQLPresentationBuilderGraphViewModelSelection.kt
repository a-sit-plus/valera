package ui.presentation

import androidx.compose.runtime.saveable.listSaver
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier

sealed interface DCQLPresentationBuilderGraphViewModelSelection {
    data class SelectRequiredCredentialSetQueryOption(
        val credentialSetQueryIndex: UInt,
        val credentialSetQueryOptionIndex: UInt,
    ) : DCQLPresentationBuilderGraphViewModelSelection

    data class SelectOptionalCredentialSetQueryOption(
        val credentialSetQueryIndex: UInt,
        val credentialSetQueryOptionIndex: UInt?,
    ) : DCQLPresentationBuilderGraphViewModelSelection

    data class SelectSubmissions(
        val queryIdentifier: DCQLCredentialQueryIdentifier,
        val submissionIndices: Set<UInt>,
    ) : DCQLPresentationBuilderGraphViewModelSelection

    data object ContinueWithSelection : DCQLPresentationBuilderGraphViewModelSelection

    companion object {
        val Saver = listSaver(
            save = { selection ->
                when (selection) {
                    is SelectRequiredCredentialSetQueryOption -> listOf(
                        0L,
                        selection.credentialSetQueryIndex.toLong(),
                        selection.credentialSetQueryOptionIndex.toLong()
                    )

                    is SelectOptionalCredentialSetQueryOption -> listOf(
                        1L,
                        selection.credentialSetQueryIndex.toLong(),
                        selection.credentialSetQueryOptionIndex?.toLong() ?: -1L,
                    )

                    is SelectSubmissions -> listOf(
                        2L,
                        selection.queryIdentifier.string,
                        selection.submissionIndices.map { it.toLong() }
                    )

                    ContinueWithSelection -> listOf(3L)
                }
            },

            restore = { data ->
                @Suppress("UNCHECKED_CAST")
                when (data[0] as Long) {
                    0L -> SelectRequiredCredentialSetQueryOption(
                        credentialSetQueryIndex = (data[1] as Long).toUInt(),
                        credentialSetQueryOptionIndex = (data[2] as Long).toUInt()
                    )

                    1L -> SelectOptionalCredentialSetQueryOption(
                        credentialSetQueryIndex = (data[1] as Long).toUInt(),
                        credentialSetQueryOptionIndex = (data[2] as Long).takeIf {
                            it >= 0
                        }?.toUInt()
                    )

                    2L -> SelectSubmissions(
                        queryIdentifier = DCQLCredentialQueryIdentifier(data[1] as String),
                        submissionIndices = (data[2] as List<Long>).map {
                            it.toUInt()
                        }.toSet()
                    )

                    3L -> ContinueWithSelection

                    else -> error("Unknown selection type")
                }
            }
        )
    }
}
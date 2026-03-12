package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import at.asitplus.data.NonEmptyList
import at.asitplus.data.NonEmptyList.Companion.toNonEmptyList
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.text_label_select_no_optional_credential_set_query_option
import org.jetbrains.compose.resources.stringResource

@ExperimentalMaterial3Api
@Composable
fun DCQLPresentationOptionalCredentialSetQueryOptionSelectionPageContent(
    credentialSetQueryOptionUiModels: NonEmptyList<CredentialSetQueryOptionUiModel>,
    onAbort: () -> Unit,
    onContinue: (() -> Unit)?,
    selectedOptionIndex: UInt?,
    onSetSelectedOptionIndex: (UInt?) -> Unit
) {
    DCQLPresentationCredentialSetQueryOptionSelectionPageContent(
        isCredentialSetQueryRequired = false,
        selectedOptionIndex = selectedOptionIndex?.plus(1u) ?: 0u,
        credentialSetQueryOptionUiModels = listOf(
            CredentialSetQueryOptionUiModel(
                isSatisfiable = true,
                credentialQueries = listOf(
                    DCQLCredentialQueryUiModel(
                        credentialRepresentationLocalized = null,
                        credentialSchemeLocalized = stringResource(Res.string.text_label_select_no_optional_credential_set_query_option),
                        requestedAttributesLocalized = DCQLCredentialQueryUiModelAttributeLabels(
                            attributesLocalized = listOf(),
                            otherAttributes = 0,
                        )
                    )
                )
            )
        ).plus(credentialSetQueryOptionUiModels).toNonEmptyList(),
        onSelectCredentialSetQueryOptionAtIndex = {
            onSetSelectedOptionIndex(it.takeIf {
                it > 0u
            }?.minus(1u))
        },
        onAbort = onAbort,
        onContinue = onContinue
    )
}
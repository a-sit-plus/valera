package ui.composables.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_all_missing_data
import at.asitplus.valera.resources.section_heading_load_data_selection
import at.asitplus.wallet.app.common.credentialScheme
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import at.asitplus.wallet.lib.oidvci.toRepresentation
import data.PersonalDataCategory
import data.credentials.CredentialAttributeCategorization
import org.jetbrains.compose.resources.stringResource
import ui.composables.CategorySelectionRow
import ui.composables.LabeledTriStateCheckbox
import ui.state.toggleableState


@Composable
fun CredentialAttributeSelectionForm(
    credentialIdentifierInfo: CredentialIdentifierInfo,
    requestedAttributes: Set<NormalizedJsonPath>,
    onChangeRequestedAttributes: ((Set<NormalizedJsonPath>) -> Unit)?,
    attributeCategoriesExpanded: Map<PersonalDataCategory, Boolean>,
    onSetAttributeCategoriesExpanded: ((Pair<PersonalDataCategory, Boolean>) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val attributeCategorization = CredentialAttributeCategorization.load(
        credentialIdentifierInfo.credentialScheme,
        credentialIdentifierInfo.supportedCredentialFormat.format.toRepresentation()
    ).sourceAttributeCategorization

    Column(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(Res.string.section_heading_load_data_selection),
            style = MaterialTheme.typography.titleMedium,
        )
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Row(
                modifier = ui.composables.CategorySelectionRowDefaults.modifier,
            ) {
                attributeCategorization.values.flatten().map {
                    requestedAttributes.contains(it)
                }.toggleableState.let { state ->
                    LabeledTriStateCheckbox(
                        label = stringResource(Res.string.button_label_all_missing_data),
                        state = state,
                        onClick = {
                            if (state == ToggleableState.On) {
                                onChangeRequestedAttributes?.invoke(
                                    requestedAttributes - attributeCategorization.values.flatten()
                                        .toSet()
                                )
                            } else {
                                onChangeRequestedAttributes?.invoke(
                                    requestedAttributes + attributeCategorization.values.flatten()
                                )
                            }
                        },
                        enabled = onSetAttributeCategoriesExpanded != null,
                        gapWidth = 16.dp,
                        labelTextStyle = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        attributeCategorization.filter {
            it.value.isNotEmpty()
        }.forEach { attributeCategory ->
            CategorySelectionRow(
                attributeCategory = attributeCategory,
                isExpanded = attributeCategoriesExpanded[attributeCategory.key] ?: true,
                onToggleExpanded = { onSetAttributeCategoriesExpanded?.invoke(attributeCategory.key to it) },
                isEditSelectionEnabled = onSetAttributeCategoriesExpanded != null,
                requestedCredentialScheme = credentialIdentifierInfo.credentialScheme,
                requestedAttributes = requestedAttributes,
                onChangeRequestedAttributes = onChangeRequestedAttributes,
            )
        }
    }
}
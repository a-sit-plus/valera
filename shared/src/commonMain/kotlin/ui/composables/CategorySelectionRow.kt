package ui.composables

import ExpandButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.content_description_hide_attributes
import at.asitplus.valera.resources.content_description_show_attributes
import at.asitplus.wallet.lib.data.ConstantIndex
import data.PersonalDataCategory
import data.credentials.CredentialAttributeTranslator
import org.jetbrains.compose.resources.stringResource
import ui.state.toggleableState


@Composable
fun CategorySelectionRow(
    attributeCategory: Map.Entry<PersonalDataCategory, List<NormalizedJsonPath>>,
    isExpanded: Boolean,
    onToggleExpanded: (Boolean) -> Unit,
    requestedCredentialScheme: ConstantIndex.CredentialScheme?,
    requestedAttributes: Set<NormalizedJsonPath>,
    onChangeRequestedAttributes: ((Set<NormalizedJsonPath>) -> Unit)?,
    isEditSelectionEnabled: Boolean = true,
) {
    val attributeTranslator = CredentialAttributeTranslator[requestedCredentialScheme]
    val categoryAttributes = attributeCategory.value
    categoryAttributes.map { requestedAttributes.contains(it) }.toggleableState.let { state ->
        CategorySelectionRow(
            label = stringResource(attributeCategory.key.categoryTitle),
            state = state,
            onClick = {
                if (state == ToggleableState.On) {
                    onChangeRequestedAttributes?.invoke(requestedAttributes - categoryAttributes)
                } else {
                    onChangeRequestedAttributes?.invoke(requestedAttributes + categoryAttributes)
                }
            },
            isExpanded = isExpanded,
            onToggleExpanded = {
                onToggleExpanded(!isExpanded)
            },
            attributeSelections = attributeCategory.value.map {
                AttributeSelection(
                    attributeLabel = attributeTranslator?.translate(it)?.let { stringResource(it) }
                        ?: it.toString(),
                    isSelected = requestedAttributes.contains(it),
                )
            },
            onToggleAttributeSelection = { index ->
                val attribute = categoryAttributes[index]
                if (requestedAttributes.contains(attribute)) {
                    onChangeRequestedAttributes?.invoke(requestedAttributes - attribute)
                } else {
                    onChangeRequestedAttributes?.invoke(requestedAttributes + attribute)
                }
            },
            isEditSelectionEnabled = onChangeRequestedAttributes != null && isEditSelectionEnabled,
        )
    }
}

@Composable
fun CategorySelectionRow(
    label: String,
    state: ToggleableState,
    onClick: () -> Unit,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    attributeSelections: List<AttributeSelection>,
    onToggleAttributeSelection: (index: Int) -> Unit,
    isEditSelectionEnabled: Boolean = true,
    modifier: Modifier = CategorySelectionRowDefaults.modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LabeledTriStateCheckbox(
            label = label,
            state = state,
            onClick = onClick,
            enabled = isEditSelectionEnabled,
            gapWidth = 16.dp,
            labelTextStyle = MaterialTheme.typography.bodyLarge,
        )
        ExpandButton(
            isExpanded = isExpanded,
            onClick = onToggleExpanded,
            contentDescription = stringResource(
                if (isExpanded) {
                    Res.string.content_description_hide_attributes
                } else {
                    Res.string.content_description_show_attributes
                }
            )
        )
    }
    if (isExpanded) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        ) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 8.dp, end = 0.dp, bottom = 8.dp, start = 34.dp)
            ) {
                // source: https://stackoverflow.com/questions/70724196/how-to-create-bulleted-text-list-in-android-jetpack-compose
                attributeSelections.forEachIndexed { index, it ->
                    LabeledCheckbox(
                        label = it.attributeLabel,
                        checked = it.isSelected,
                        onCheckedChange = {
                            onToggleAttributeSelection(index)
                        },
                        enabled = isEditSelectionEnabled,
                        labelTextStyle = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}

class CategorySelectionRowDefaults {
    companion object {
        val modifier = Modifier.fillMaxWidth().padding(
            top = 8.dp,
            end = 24.dp,
            bottom = 8.dp,
            start = 16.dp,
        )
    }
}

data class AttributeSelection(
    val attributeLabel: String,
    val isSelected: Boolean,
)
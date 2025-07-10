package ui.views.iso.verifier.requests

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_check_age
import at.asitplus.valera.resources.button_label_check_license
import at.asitplus.valera.resources.button_label_check_over_age
import at.asitplus.valera.resources.section_heading_request_mdl
import at.asitplus.valera.resources.text_label_all_attributes
import at.asitplus.valera.resources.text_label_mandatory_attributes
import data.document.SelectableAge
import data.document.SelectableRequest
import data.document.SelectableRequestType
import org.jetbrains.compose.resources.stringResource

@Composable
fun MDLRequests(
    layoutSpacingModifier: Modifier,
    listSpacingModifier: Modifier,
    onRequestSelected: (SelectableRequest) -> Unit
) {
    val showDropDownAge = remember { mutableStateOf(false) }

    Column(modifier = layoutSpacingModifier) {
        Text(
            text = stringResource(Res.string.section_heading_request_mdl),
            style = MaterialTheme.typography.titleMedium
        )
        RequestItem(
            icon = { Icon(imageVector = Icons.Outlined.CreditCard, contentDescription = null) },
            label = stringResource(Res.string.button_label_check_license),
            subLabel = stringResource(Res.string.text_label_mandatory_attributes),
            onClick = { onRequestSelected(SelectableRequest(SelectableRequestType.MDL_MANDATORY)) },
            modifier = listSpacingModifier.fillMaxWidth()
        )
        RequestItem(
            icon = { Icon(imageVector = Icons.Outlined.CreditCard, contentDescription = null) },
            label = stringResource(Res.string.button_label_check_license),
            subLabel = stringResource(Res.string.text_label_all_attributes),
            onClick = { onRequestSelected(SelectableRequest(SelectableRequestType.MDL_FULL)) },
            modifier = listSpacingModifier.fillMaxWidth()
        )
        RequestItem(
            icon = { Icon(imageVector = Icons.Outlined.Cake, contentDescription = null) },
            label = stringResource(Res.string.button_label_check_age),
            onClick = { showDropDownAge.value = !showDropDownAge.value },
            modifier = listSpacingModifier.fillMaxWidth(),
        )
        if (showDropDownAge.value) {
            Column {
                SelectableAge.valuesList.forEach { age ->
                    RequestItem(
                        label = stringResource(Res.string.button_label_check_over_age, age),
                        onClick = { onRequestSelected(SelectableRequest(SelectableRequestType.MDL_AGE_VERIFICATION, age)) },
                        modifier = listSpacingModifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

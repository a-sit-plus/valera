package ui.views.iso.verifier.requests

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Icon
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

    val requestItems = listOf(
        RequestItemData(
            icon = { Icon(imageVector = Icons.Outlined.CreditCard, contentDescription = null) },
            label = stringResource(Res.string.button_label_check_license),
            subLabel = stringResource(Res.string.text_label_mandatory_attributes),
            onClick = { onRequestSelected(SelectableRequest(SelectableRequestType.MDL_MANDATORY)) }
        ),
        RequestItemData(
            icon = { Icon(imageVector = Icons.Outlined.CreditCard, contentDescription = null) },
            label = stringResource(Res.string.button_label_check_license),
            subLabel = stringResource(Res.string.text_label_all_attributes),
            onClick = { onRequestSelected(SelectableRequest(SelectableRequestType.MDL_FULL)) }
        ),
        RequestItemData(
            icon = { Icon(imageVector = Icons.Outlined.Cake, contentDescription = null) },
            label = stringResource(Res.string.button_label_check_age),
            onClick = { showDropDownAge.value = !showDropDownAge.value }
        )
    )

    RequestSection(
        title = stringResource(Res.string.section_heading_request_mdl),
        layoutSpacingModifier = layoutSpacingModifier,
        listSpacingModifier = listSpacingModifier,
        requestItems = requestItems,
        extraContent = {
            if (showDropDownAge.value) {
                Column {
                    SelectableAge.valuesList.forEach { age ->
                        RequestItem(
                            requestItemData = RequestItemData(
                                label = stringResource(Res.string.button_label_check_over_age, age),
                                onClick = {
                                    onRequestSelected(
                                        SelectableRequest(
                                            SelectableRequestType.MDL_AGE_VERIFICATION,
                                            age
                                        )
                                    )
                                }
                            ),
                            modifier = listSpacingModifier
                        )
                    }
                }
            }
        }
    )
}

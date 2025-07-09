package ui.views.iso.verifier.requests

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_check_age
import at.asitplus.valera.resources.button_label_check_identity
import at.asitplus.valera.resources.button_label_check_over_age
import at.asitplus.valera.resources.section_heading_request_pid
import at.asitplus.valera.resources.text_label_all_attributes
import at.asitplus.valera.resources.text_label_mandatory_attributes
import at.asitplus.wallet.app.common.iso.transfer.DeviceEngagementMethods
import data.document.SelectableAge
import org.jetbrains.compose.resources.stringResource
import ui.viewmodels.iso.VerifierViewModel

@Composable
fun PIDRequests(
    layoutSpacingModifier: Modifier,
    vm: VerifierViewModel,
    selectedEngagementMethod: DeviceEngagementMethods,
    listSpacingModifier: Modifier,
) {
    val showDropDownAge = remember { mutableStateOf(false) }

    Column(modifier = layoutSpacingModifier) {
        Text(
            text = stringResource(Res.string.section_heading_request_pid),
            style = MaterialTheme.typography.titleMedium
        )
        RequestItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null
                )
            },
            label = stringResource(Res.string.button_label_check_identity),
            subLabel = stringResource(Res.string.text_label_mandatory_attributes),
            onClick = { vm.onClickPredefinedPidRequiredAttributes(selectedEngagementMethod) },
            modifier = listSpacingModifier.fillMaxWidth()
        )
        RequestItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null
                )
            },
            label = stringResource(Res.string.button_label_check_identity),
            subLabel = stringResource(Res.string.text_label_all_attributes),
            onClick = { vm.onClickPredefinedPidFullAttributes(selectedEngagementMethod) },
            modifier = listSpacingModifier.fillMaxWidth()
        )
        RequestItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Cake,
                    contentDescription = null
                )
            },
            label = stringResource(Res.string.button_label_check_age),
            onClick = { showDropDownAge.value = !showDropDownAge.value },
            modifier = listSpacingModifier.fillMaxWidth(),
        )
        if (showDropDownAge.value) {
            Column {
                SelectableAge.valuesList.forEach { age ->
                    RequestItem(
                        label = stringResource(Res.string.button_label_check_over_age, age),
                        onClick = { vm.onClickPredefinedAgePid(age, selectedEngagementMethod) },
                        modifier = listSpacingModifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

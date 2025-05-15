package ui.views.iso.verifier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_check_age
import at.asitplus.valera.resources.button_label_check_custom
import at.asitplus.valera.resources.button_label_check_identity
import at.asitplus.valera.resources.button_label_check_license
import at.asitplus.valera.resources.button_label_check_over_age
import at.asitplus.valera.resources.heading_label_select_data_retrieval_screen
import at.asitplus.valera.resources.section_heading_request_custom
import at.asitplus.valera.resources.section_heading_request_engagement_method
import at.asitplus.valera.resources.section_heading_request_mdl
import at.asitplus.valera.resources.section_heading_request_pid
import at.asitplus.valera.resources.text_label_all_attributes
import at.asitplus.valera.resources.text_label_mandatory_attributes
import at.asitplus.wallet.app.common.iso.transfer.DeviceEngagementMethods
import data.document.SelectableAge
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.viewmodels.iso.VerifierViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifierDocumentSelectionView(
    vm: VerifierViewModel,
    bottomBar: @Composable () -> Unit
) {
    val listSpacingModifier = Modifier.padding(top = 8.dp)
    val layoutSpacingModifier = Modifier.padding(top = 24.dp)
    val showDropDownMdlAge = remember { mutableStateOf(false) }
    val showDropDownPidAge = remember { mutableStateOf(false) }

    val engagementMethods = DeviceEngagementMethods.entries
    var selectedEngagementMethod by remember { mutableStateOf(DeviceEngagementMethods.NFC) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ScreenHeading(
                            stringResource(Res.string.heading_label_select_data_retrieval_screen),
                            Modifier.weight(1f)
                        )
                    }
                },
                actions = {
                    Logo(onClick = vm.onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = vm.onClickSettings)) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    }
                    Spacer(Modifier.width(15.dp))
                }
            )
        },
        bottomBar = { bottomBar() }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Column(
                modifier = Modifier.padding(end = 16.dp, start = 16.dp, bottom = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = layoutSpacingModifier,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(Res.string.section_heading_request_engagement_method),
                        style = MaterialTheme.typography.titleMedium
                    )
                    engagementMethods.forEach { engagementMethod ->
                        singleChoiceButton(
                            engagementMethod.friendlyName,
                            selectedEngagementMethod.friendlyName,
                            modifier = listSpacingModifier.fillMaxWidth(),
                            icon = {
                                Icon(
                                    imageVector = engagementMethod.icon,
                                    contentDescription = null
                                )
                            }
                        ) {
                            selectedEngagementMethod = engagementMethod
                        }
                    }
                }
                Column(modifier = layoutSpacingModifier) {
                    Text(
                        text = stringResource(Res.string.section_heading_request_mdl),
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.CreditCard,
                                contentDescription = null
                            )
                        },
                        label = stringResource(Res.string.button_label_check_license),
                        subLabel = stringResource(Res.string.text_label_mandatory_attributes),
                        onClick = {
                            vm.onClickPredefinedMdlMandatoryAttributes(selectedEngagementMethod)
                        },
                        modifier = listSpacingModifier.fillMaxWidth()
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.CreditCard,
                                contentDescription = null
                            )
                        },
                        label = stringResource(Res.string.button_label_check_license),
                        subLabel = stringResource(Res.string.text_label_all_attributes),
                        onClick = { vm.onClickPredefinedMdlFullAttributes(selectedEngagementMethod) },
                        modifier = listSpacingModifier.fillMaxWidth()
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Cake,
                                contentDescription = null
                            )
                        },
                        label = stringResource(Res.string.button_label_check_age),
                        onClick = { showDropDownMdlAge.value = !showDropDownMdlAge.value },
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    if (showDropDownMdlAge.value) {
                        Column {
                            SelectableAge.valuesList.forEach { age ->
                                TextIconButtonListItem(
                                    label = stringResource(Res.string.button_label_check_over_age, age),
                                    onClick = {
                                        vm.onClickPredefinedAgeMdl(age, selectedEngagementMethod)
                                    },
                                    modifier = listSpacingModifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                Column(modifier = layoutSpacingModifier) {
                    Text(
                        text = stringResource(Res.string.section_heading_request_pid),
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null
                            )
                        },
                        label = stringResource(Res.string.button_label_check_identity),
                        subLabel = stringResource(Res.string.text_label_mandatory_attributes),
                        onClick = {
                            vm.onClickPredefinedPidRequiredAttributes(selectedEngagementMethod)
                        },
                        modifier = listSpacingModifier.fillMaxWidth()
                    )
                    TextIconButtonListItem(
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
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Cake,
                                contentDescription = null
                            )
                        },
                        label = stringResource(Res.string.button_label_check_age),
                        onClick = { showDropDownPidAge.value = !showDropDownPidAge.value },
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    if (showDropDownPidAge.value) {
                        Column {
                            SelectableAge.valuesList.forEach { age ->
                                TextIconButtonListItem(
                                    label = stringResource(Res.string.button_label_check_over_age, age),
                                    onClick = {
                                        vm.onClickPredefinedAgePid(age, selectedEngagementMethod)
                                    },
                                    modifier = listSpacingModifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                Column(modifier = layoutSpacingModifier) {
                    Text(
                        text = stringResource(Res.string.section_heading_request_custom),
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Build,
                                contentDescription = null
                            )
                        },
                        label = stringResource(Res.string.button_label_check_custom),
                        onClick = { vm.navigateToCustomSelectionView(selectedEngagementMethod) },
                        modifier = listSpacingModifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TextIconButtonListItem(
    icon: @Composable () -> Unit = {},
    label: String,
    subLabel: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(onClick = onClick)
            .padding(top = 4.dp, end = 16.dp, bottom = 4.dp, start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            subLabel?.let {
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

package ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.button_label_check_over_age
import composewalletapp.shared.generated.resources.button_label_check_identity
import composewalletapp.shared.generated.resources.button_label_check_age
import composewalletapp.shared.generated.resources.button_label_check_license
import composewalletapp.shared.generated.resources.heading_label_select_data_retrieval_screen
import composewalletapp.shared.generated.resources.button_label_check_custom
import composewalletapp.shared.generated.resources.section_heading_request_eausweiße
import composewalletapp.shared.generated.resources.section_heading_request_license
import composewalletapp.shared.generated.resources.section_heading_request_custom
import data.bletransfer.Verifier
import data.bletransfer.verifier.getAgeVerificationDocument
import data.bletransfer.verifier.getIdentityDocument
import data.bletransfer.verifier.getLicenseDocument
import org.jetbrains.compose.resources.stringResource

@Composable
fun SelectDataRetrievalScreen(
    navigateToCustomSelectionPage: () -> Unit,
    navigateToQrDeviceEngagementPage: (Verifier.Document) -> Unit,
    ) {
    SelectDataRetrievalView(
        onClickPreDefined=navigateToQrDeviceEngagementPage,
        onClickCustom=navigateToCustomSelectionPage
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectDataRetrievalView(
    onClickPreDefined: (Verifier.Document) -> Unit,
    onClickCustom: () -> Unit
) {
    var showDropDown = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.heading_label_select_data_retrieval_screen),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
            )
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Column(
                modifier = Modifier.padding(end = 16.dp, start = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val layoutSpacingModifier = Modifier.padding(top = 24.dp)
                Column(
                    modifier = layoutSpacingModifier
                ) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = stringResource(Res.string.section_heading_request_eausweiße),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                            )
                        },
                        label = stringResource(Res.string.button_label_check_identity),
                        onClick = {onClickPreDefined(getIdentityDocument())},
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.CreditCard,
                                contentDescription = null,
                            )
                        },
                        label = stringResource(Res.string.button_label_check_license),
                        onClick = {onClickPreDefined(getLicenseDocument())},
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Cake,
                                contentDescription = null,
                            )
                        },
                        label = stringResource(Res.string.button_label_check_age),
                        onClick = {
                            showDropDown.value = !showDropDown.value
                        },
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    if (showDropDown.value) {
                        Column {
                            for (age in intArrayOf(14, 16, 18, 21)) {
                                TextIconButtonListItem(
                                    icon = {},
                                    label = stringResource(Res.string.button_label_check_over_age) + age,
                                    onClick = {onClickPreDefined(getAgeVerificationDocument(age))},
                                    modifier = listSpacingModifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = layoutSpacingModifier
                ) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = stringResource(Res.string.section_heading_request_license),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.CreditCard,
                                contentDescription = null,
                            )
                        },
                        label = stringResource(Res.string.button_label_check_license),
                        onClick = {onClickPreDefined(getLicenseDocument())},
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                }
                Column(
                    modifier = layoutSpacingModifier
                ) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = stringResource(Res.string.section_heading_request_custom),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Build,
                                contentDescription = null,
                            )
                        },
                        label = stringResource(Res.string.button_label_check_custom),
                        onClick = onClickCustom,
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}


@Composable
private fun TextIconButtonListItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gap = 16.dp
    Row(
        modifier = modifier.clickable(
            onClick = onClick,
        ).padding(top = 8.dp, end = 24.dp, bottom = 8.dp, start = 16.dp),
    ) {
        icon()
        Spacer(modifier = Modifier.width(gap))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}



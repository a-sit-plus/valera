package ui.views.iso

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
import at.asitplus.valera.resources.section_heading_request_eausweise
import at.asitplus.valera.resources.section_heading_request_license
import data.bletransfer.Verifier
import data.bletransfer.verifier.getAgeVerificationDocument
import data.bletransfer.verifier.getIdentityDocument
import data.bletransfer.verifier.getLicenseDocument
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyDataView(
    onClickPreDefined: (Verifier.Document) -> Unit,
    onClickCustom: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val showDropDown = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(Res.string.heading_label_select_data_retrieval_screen),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Logo()
                    Spacer(Modifier.width(8.dp))
                }
            })
        },
        bottomBar = { bottomBar() }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Column(
                modifier = Modifier.padding(end = 16.dp, start = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val layoutSpacingModifier = Modifier.padding(top = 24.dp)
                Column(modifier = layoutSpacingModifier) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = stringResource(Res.string.section_heading_request_eausweise),
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
                        onClick = { onClickPreDefined(getIdentityDocument()) },
                        modifier = listSpacingModifier.fillMaxWidth()
                    )
                    TextIconButtonListItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.CreditCard,
                                contentDescription = null,
                            )
                        },
                        label = stringResource(Res.string.button_label_check_license),
                        onClick = { onClickPreDefined(getLicenseDocument()) },
                        modifier = listSpacingModifier.fillMaxWidth()
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
                                    onClick = { onClickPreDefined(getAgeVerificationDocument(age)) },
                                    modifier = listSpacingModifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                Column(modifier = layoutSpacingModifier) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = stringResource(Res.string.section_heading_request_license),
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
                        onClick = { onClickPreDefined(getLicenseDocument()) },
                        modifier = listSpacingModifier.fillMaxWidth()
                    )
                }
                Column(modifier = layoutSpacingModifier) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
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
                        onClick = onClickCustom,
                        modifier = listSpacingModifier.fillMaxWidth()
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
        modifier = modifier.clickable(onClick = onClick)
            .padding(top = 8.dp, end = 24.dp, bottom = 8.dp, start = 16.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(gap))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

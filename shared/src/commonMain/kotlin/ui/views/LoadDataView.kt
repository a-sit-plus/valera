package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.heading_label_load_data_screen
import composewalletapp.shared.generated.resources.heading_label_refresh_data_screen
import composewalletapp.shared.generated.resources.info_text_redirection_to_id_austria_for_credential_provisioning
import composewalletapp.shared.generated.resources.section_heading_configuration
import data.attributeTranslation
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.DataCategoryDisplaySection
import ui.composables.LabeledCheckbox
import ui.composables.PersonalDataCategory
import ui.composables.buttons.LoadDataButton
import ui.composables.buttons.NavigateUpButton
import ui.composables.buttons.RefreshDataButton
import ui.composables.inputFields.IssuingServiceInputField
import ui.composables.inputFields.StatefulCredentialRepresentationInputField
import ui.composables.inputFields.StatefulCredentialSchemeInputField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun LoadDataView(
    host: TextFieldValue,
    onChangeHost: (TextFieldValue) -> Unit,
    credentialRepresentation: ConstantIndex.CredentialRepresentation,
    onChangeCredentialRepresentation: (ConstantIndex.CredentialRepresentation) -> Unit,
    credentialScheme: ConstantIndex.CredentialScheme,
    onChangeCredentialScheme: (ConstantIndex.CredentialScheme) -> Unit,
    availableCredentials: Collection<SubjectCredentialStore.StoreEntry>,
    requestedAttributes: Collection<String>,
    onChangeRequestedAttributes: (Collection<String>) -> Unit,
    navigateUp: (() -> Unit)? = null,
    refreshData: () -> Unit,
) {
    val attributeCategories = listOf(
        PersonalDataCategory.IdentityData to PersonalDataCategory.IdentityData.attributes,
        PersonalDataCategory.ResidenceData to PersonalDataCategory.ResidenceData.attributes,
        PersonalDataCategory.AgeData to PersonalDataCategory.AgeData.attributes,
        PersonalDataCategory.DrivingPermissions to PersonalDataCategory.DrivingPermissions.attributes,
        PersonalDataCategory.AdmissionData to PersonalDataCategory.AdmissionData.attributes,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (availableCredentials.isNotEmpty()) {
                                Res.string.heading_label_load_data_screen
                            } else {
                                Res.string.heading_label_refresh_data_screen
                            }
                        ),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = {
                    if (navigateUp != null) {
                        NavigateUpButton(navigateUp)
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    if(availableCredentials.isNotEmpty()) {
                        RefreshDataButton(
                            onClick = refreshData
                        )
                    } else {
                        LoadDataButton(
                            onClick = refreshData
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val columnSpacingModifier = Modifier.padding(top = 32.dp)
                Column {
                    Text(
                        stringResource(Res.string.info_text_redirection_to_id_austria_for_credential_provisioning),
                    )
                    Column {
                        val listSpacingModifier = Modifier.padding(top = 8.dp)
                        Text(
                            text = stringResource(Res.string.section_heading_configuration),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )

                        IssuingServiceInputField(
                            value = host,
                            onValueChange = onChangeHost,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                        StatefulCredentialRepresentationInputField(
                            value = credentialRepresentation,
                            onValueChange = onChangeCredentialRepresentation,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                        StatefulCredentialSchemeInputField(
                            value = credentialScheme,
                            onValueChange = onChangeCredentialScheme,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                    }
                }
                Column(
                    modifier = columnSpacingModifier,
                ) {
                    DataCategoryDisplaySection(
                        title = "Bestehende Daten",
                        attributes = listOf(

                        )
                    )
                    DataCategoryDisplaySection(
                        title = "Neue Daten",
                        attributes = listOf(

                        )
                    )
                }
            }
        }
//        items(
//            count = credentialScheme.claimNames.size,
//            key = { index -> credentialScheme.claimNames.toList()[index] }
//        ) { index ->
//            DataCategoryDisplaySection(
//                title = "Neue Daten laden",
//                attributes = listOf(
//
//                )
//            )
//            LabeledCheckbox(
//                label = credentialScheme.claimNames.toList()[index].let {
//                    it.attributeTranslation?.let { translation ->
//                        stringResource(translation)
//                    } ?: it
//                },
//                checked = requestedAttributes.contains(credentialScheme.claimNames.toList()[index]),
//                onCheckedChange = {
//                    onChangeRequestedAttributes(
//                        if (it) {
//                            requestedAttributes + credentialScheme.claimNames.toList()[index]
//                        } else {
//                            requestedAttributes - credentialScheme.claimNames.toList()[index]
//                        }
//                    )
//                }
//            )
//        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun LoadDataVie2w(
    host: TextFieldValue,
    onChangeHost: (TextFieldValue) -> Unit,
    credentialRepresentation: ConstantIndex.CredentialRepresentation,
    onChangeCredentialRepresentation: (ConstantIndex.CredentialRepresentation) -> Unit,
    credentialScheme: ConstantIndex.CredentialScheme,
    onChangeCredentialScheme: (ConstantIndex.CredentialScheme) -> Unit,
    requestedAttributes: Collection<String>,
    onChangeRequestedAttributes: (Collection<String>) -> Unit,
    navigateUp: (() -> Unit)? = null,
    refreshData: () -> Unit,
) {
    val attributeCategories = listOf(
        PersonalDataCategory.IdentityData to PersonalDataCategory.IdentityData.attributes,
        PersonalDataCategory.ResidenceData to PersonalDataCategory.ResidenceData.attributes,
        PersonalDataCategory.AgeData to PersonalDataCategory.AgeData.attributes,
        PersonalDataCategory.DrivingPermissions to PersonalDataCategory.DrivingPermissions.attributes,
        PersonalDataCategory.AdmissionData to PersonalDataCategory.AdmissionData.attributes,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.heading_label_load_data_screen),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = {
                    if (navigateUp != null) {
                        NavigateUpButton(navigateUp)
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    LoadDataButton(
                        onClick = refreshData
                    )

                }
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding)
        ) {
            LazyColumn(
                state = rememberLazyListState()
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            stringResource(Res.string.info_text_redirection_to_id_austria_for_credential_provisioning),
                        )
                        val listSpacingModifier = Modifier.padding(top = 8.dp)
                        Text(
                            text = stringResource(Res.string.section_heading_configuration),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )

                        IssuingServiceInputField(
                            value = host,
                            onValueChange = onChangeHost,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                        StatefulCredentialRepresentationInputField(
                            value = credentialRepresentation,
                            onValueChange = onChangeCredentialRepresentation,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                        StatefulCredentialSchemeInputField(
                            value = credentialScheme,
                            onValueChange = onChangeCredentialScheme,
                            modifier = listSpacingModifier.fillMaxWidth(),
                        )
                    }
                }
                items(
                    count = credentialScheme.claimNames.size,
                    key = { index -> credentialScheme.claimNames.toList()[index] }
                ) { index ->
                    DataCategoryDisplaySection(
                        title = "Neue Daten laden",
                        attributes = listOf(

                        )
                    )
                    LabeledCheckbox(
                        label = credentialScheme.claimNames.toList()[index].let {
                            it.attributeTranslation?.let { translation ->
                                stringResource(translation)
                            } ?: it
                        },
                        checked = requestedAttributes.contains(credentialScheme.claimNames.toList()[index]),
                        onCheckedChange = {
                            onChangeRequestedAttributes(
                                if (it) {
                                    requestedAttributes + credentialScheme.claimNames.toList()[index]
                                } else {
                                    requestedAttributes - credentialScheme.claimNames.toList()[index]
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

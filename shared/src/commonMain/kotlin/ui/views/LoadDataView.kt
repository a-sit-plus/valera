package ui.views

import ExpandButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.button_label_all_available_data
import composewalletapp.shared.generated.resources.button_label_all_missing_data
import composewalletapp.shared.generated.resources.content_description_hide_attributes
import composewalletapp.shared.generated.resources.content_description_show_attributes
import composewalletapp.shared.generated.resources.heading_label_load_data_screen
import composewalletapp.shared.generated.resources.heading_label_refresh_data_screen
import composewalletapp.shared.generated.resources.info_text_redirection_to_id_austria_for_credential_provisioning
import composewalletapp.shared.generated.resources.section_heading_available_data
import composewalletapp.shared.generated.resources.section_heading_configuration
import composewalletapp.shared.generated.resources.section_heading_missing_data
import data.CredentialExtractor
import data.attributeTranslation
import data.storage.scheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.LabeledCheckbox
import ui.composables.LabeledTriStateCheckbox
import ui.composables.PersonalDataCategory
import ui.composables.attributeCategorizationOrder
import ui.composables.buttons.LoadDataButton
import ui.composables.buttons.NavigateUpButton
import ui.composables.buttons.RefreshDataButton
import ui.composables.inputFields.IssuingServiceInputField
import ui.composables.inputFields.StatefulCredentialRepresentationInputField
import ui.composables.inputFields.StatefulCredentialSchemeInputField

@Composable
fun StatefulLoadDataView(
    isEditEnabled: Boolean,
    host: TextFieldValue,
    onChangeHost: (TextFieldValue) -> Unit,
    credentialRepresentation: ConstantIndex.CredentialRepresentation,
    onChangeCredentialRepresentation: (ConstantIndex.CredentialRepresentation) -> Unit,
    credentialScheme: ConstantIndex.CredentialScheme,
    onChangeCredentialScheme: (ConstantIndex.CredentialScheme) -> Unit,
    availableCredentials: Collection<SubjectCredentialStore.StoreEntry>,
    requestedAttributes: Set<String>,
    onChangeRequestedAttributes: (Set<String>) -> Unit,
    navigateUp: (() -> Unit)? = null,
    refreshData: () -> Unit,
) {
    val attributeCategorization = attributeCategorizationOrder.map { category ->
        val credentialSchemeAttributes = category.attributes[credentialScheme] ?: listOf()
        credentialSchemeAttributes.let { category to it }
    }.toMap()

    val fullAttributeCategorization = attributeCategorization + Pair(
        PersonalDataCategory.OtherData,
        credentialScheme.claimNames.filter {
            attributeCategorization.values.flatten().contains(it) == false
        },
    )

    val availableCredentialsFromScheme = availableCredentials.filter {
        it.scheme == credentialScheme
    }
    val credentialExtractor = CredentialExtractor(availableCredentialsFromScheme)

    val availableAttributesCategorized = attributeCategorizationOrder.associateWith {
        fullAttributeCategorization[it]?.filter {
            credentialExtractor.containsAttribute(it)
        } ?: throw Exception("Missing category: $it")
    }
    val missingAttributesCategorized = attributeCategorizationOrder.associateWith {
        fullAttributeCategorization[it]?.filter {
            credentialExtractor.containsAttribute(it) == false
        } ?: throw Exception("Missing category: $it")
    }

    var availableAttributeCategoryExpanded by rememberSaveable(credentialScheme) {
        mutableStateOf(attributeCategorizationOrder.associateWith {
            false
        })
    }
    var missingAttributeCategoryExpanded by rememberSaveable(credentialScheme) {
        mutableStateOf(attributeCategorizationOrder.associateWith {
            false
        })
    }

    LoadDataView(
        isEditEnabled = isEditEnabled,
        host = host,
        onChangeHost = onChangeHost,
        credentialRepresentation = credentialRepresentation,
        onChangeCredentialRepresentation = onChangeCredentialRepresentation,
        credentialScheme = credentialScheme,
        onChangeCredentialScheme = onChangeCredentialScheme,
        availableCredentials = availableCredentials,
        requestedAttributes = requestedAttributes,
        onChangeRequestedAttributes = onChangeRequestedAttributes,
        availableAttributeCategoriesExpanded = availableAttributeCategoryExpanded,
        onSetAvailableAttributeCategoryExpanded = {
            availableAttributeCategoryExpanded += it
        },
        missingAttributeCategoriesExpanded = missingAttributeCategoryExpanded,
        onSetMissingAttributeCategoryExpanded = {
            missingAttributeCategoryExpanded += it
        },
        availableAttributesCategorized = availableAttributesCategorized,
        missingAttributesCategorized = missingAttributesCategorized,
        refreshData = refreshData,
        navigateUp = navigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
private fun LoadDataView(
    // state
    isEditEnabled: Boolean,
    host: TextFieldValue,
    onChangeHost: (TextFieldValue) -> Unit,
    credentialRepresentation: ConstantIndex.CredentialRepresentation,
    onChangeCredentialRepresentation: (ConstantIndex.CredentialRepresentation) -> Unit,
    credentialScheme: ConstantIndex.CredentialScheme,
    onChangeCredentialScheme: (ConstantIndex.CredentialScheme) -> Unit,
    requestedAttributes: Set<String>,
    onChangeRequestedAttributes: (Set<String>) -> Unit,
    availableAttributeCategoriesExpanded: Map<PersonalDataCategory, Boolean>,
    onSetAvailableAttributeCategoryExpanded: (Pair<PersonalDataCategory, Boolean>) -> Unit,
    missingAttributeCategoriesExpanded: Map<PersonalDataCategory, Boolean>,
    onSetMissingAttributeCategoryExpanded: (Pair<PersonalDataCategory, Boolean>) -> Unit,
    // other
    availableCredentials: Collection<SubjectCredentialStore.StoreEntry>,
    availableAttributesCategorized: Map<PersonalDataCategory, List<String>>,
    missingAttributesCategorized: Map<PersonalDataCategory, List<String>>,
    navigateUp: (() -> Unit)?,
    refreshData: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (availableCredentials.isNotEmpty()) {
                                Res.string.heading_label_refresh_data_screen
                            } else {
                                Res.string.heading_label_load_data_screen
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
                    if (availableCredentials.isNotEmpty()) {
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
                val columnSpacingModifier = Modifier.padding(top = 16.dp)
                Column {
                    Text(
                        stringResource(Res.string.info_text_redirection_to_id_austria_for_credential_provisioning),
                    )
                }
                Column(
                    modifier = columnSpacingModifier,
                ) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = stringResource(Res.string.section_heading_configuration),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    IssuingServiceInputField(
                        value = host,
                        onValueChange = onChangeHost,
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    StatefulCredentialRepresentationInputField(
                        value = credentialRepresentation,
                        onValueChange = onChangeCredentialRepresentation,
                        enabled = isEditEnabled,
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    StatefulCredentialSchemeInputField(
                        value = credentialScheme,
                        onValueChange = onChangeCredentialScheme,
                        enabled = isEditEnabled,
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                }
                if (missingAttributesCategorized.values.flatten().isNotEmpty()) {
                    Column(
                        modifier = columnSpacingModifier,
                    ) {
                        Text(
                            text = stringResource(Res.string.section_heading_missing_data),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Row(
                                modifier = CategorySelectionRowDefaults.modifier,
                            ) {
                                missingAttributesCategorized.values.flatten().map {
                                    requestedAttributes.contains(it)
                                }.toggleableState.let { state ->
                                    LabeledTriStateCheckbox(
                                        label = stringResource(Res.string.button_label_all_missing_data),
                                        state = state,
                                        onClick = {
                                            if (state == ToggleableState.On) {
                                                onChangeRequestedAttributes(
                                                    requestedAttributes - missingAttributesCategorized.values.flatten()
                                                        .toSet()
                                                )
                                            } else {
                                                onChangeRequestedAttributes(
                                                    requestedAttributes + missingAttributesCategorized.values.flatten()
                                                )
                                            }
                                        },
                                        enabled = isEditEnabled,
                                        gapWidth = 16.dp,
                                        labelTextStyle = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                        missingAttributesCategorized.filter {
                            it.value.isNotEmpty()
                        }.forEach { attributeCategory ->
                            CategorySelectionRow(
                                attributeCategory = attributeCategory,
                                isExpanded = missingAttributeCategoriesExpanded[attributeCategory.key]
                                    ?: true,
                                onToggleExpanded = {
                                    onSetMissingAttributeCategoryExpanded(
                                        Pair(
                                            attributeCategory.key,
                                            it,
                                        )
                                    )
                                },
                                isEditSelectionEnabled = isEditEnabled,
                                requestedAttributes = requestedAttributes,
                                onChangeRequestedAttributes = onChangeRequestedAttributes,
                            )
                        }
                    }
                }
                if (availableAttributesCategorized.values.flatten().isNotEmpty()) {
                    Column(
                        modifier = columnSpacingModifier,
                    ) {
                        Text(
                            text = stringResource(Res.string.section_heading_available_data),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Row(
                                modifier = CategorySelectionRowDefaults.modifier,
                            ) {
                                availableAttributesCategorized.values.flatten().map {
                                    requestedAttributes.contains(it)
                                }.toggleableState.let { state ->
                                    LabeledTriStateCheckbox(
                                        label = stringResource(Res.string.button_label_all_available_data),
                                        state = state,
                                        onClick = {
                                            if (state == ToggleableState.On) {
                                                onChangeRequestedAttributes(
                                                    requestedAttributes - availableAttributesCategorized.values.flatten()
                                                        .toSet()
                                                )
                                            } else {
                                                onChangeRequestedAttributes(
                                                    requestedAttributes + availableAttributesCategorized.values.flatten()
                                                )
                                            }
                                        },
                                        enabled = isEditEnabled,
                                        gapWidth = 16.dp,
                                        labelTextStyle = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                        availableAttributesCategorized.filter {
                            it.value.isNotEmpty()
                        }.forEach { attributeCategory ->
                            CategorySelectionRow(
                                attributeCategory = attributeCategory,
                                isExpanded = availableAttributeCategoriesExpanded[attributeCategory.key]
                                    ?: true,
                                onToggleExpanded = {
                                    onSetAvailableAttributeCategoryExpanded(
                                        Pair(
                                            attributeCategory.key,
                                            it,
                                        )
                                    )
                                },
                                isEditSelectionEnabled = isEditEnabled,
                                requestedAttributes = requestedAttributes,
                                onChangeRequestedAttributes = onChangeRequestedAttributes,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CategorySelectionRow(
    attributeCategory: Map.Entry<PersonalDataCategory, List<String>>,
    isExpanded: Boolean,
    onToggleExpanded: (Boolean) -> Unit,
    requestedAttributes: Set<String>,
    onChangeRequestedAttributes: (Set<String>) -> Unit,
    isEditSelectionEnabled: Boolean = true,
) {
    val categoryAttributes = attributeCategory.value
    categoryAttributes.map { requestedAttributes.contains(it) }.toggleableState.let { state ->
        CategorySelectionRow(
            label = stringResource(attributeCategory.key.categoryTitle),
            state = state,
            onClick = {
                if (state == ToggleableState.On) {
                    onChangeRequestedAttributes(requestedAttributes - categoryAttributes)
                } else {
                    onChangeRequestedAttributes(requestedAttributes + categoryAttributes)
                }
            },
            isExpanded = isExpanded,
            onToggleExpanded = {
                onToggleExpanded(!isExpanded)
            },
            attributeSelections = attributeCategory.value.map {
                AttributeSelection(
                    attributeLabel = it.attributeTranslation?.let {
                        stringResource(
                            it
                        )
                    } ?: it,
                    isSelected = requestedAttributes.contains(it)
                )
            },
            onToggleAttributeSelection = { index ->
                val attribute = categoryAttributes[index]
                if (requestedAttributes.contains(attribute)) {
                    onChangeRequestedAttributes(requestedAttributes - attribute)
                } else {
                    onChangeRequestedAttributes(requestedAttributes + attribute)
                }
            },
            isEditSelectionEnabled = isEditSelectionEnabled,
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
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

val List<Boolean>.toggleableState: ToggleableState
    get() = if (this.all { it }) ToggleableState.On
    else if (this.any { it }) ToggleableState.Indeterminate
    else ToggleableState.Off
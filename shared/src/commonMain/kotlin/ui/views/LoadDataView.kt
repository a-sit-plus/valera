package ui.views

import ExpandButton
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.heading_label_load_data_screen
import composewalletapp.shared.generated.resources.heading_label_refresh_data_screen
import composewalletapp.shared.generated.resources.info_text_redirection_to_id_austria_for_credential_provisioning
import composewalletapp.shared.generated.resources.section_heading_configuration
import data.CredentialExtractor
import data.attributeTranslation
import data.storage.scheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.DataCategoryDisplaySection
import ui.composables.LabeledCheckbox
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
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        text = stringResource(
//                            if (availableCredentials.isNotEmpty()) {
//                                Res.string.heading_label_load_data_screen
//                            } else {
//                                Res.string.heading_label_refresh_data_screen
//                            }
//                        ),
//                        style = MaterialTheme.typography.headlineLarge,
//                    )
//                },
//                navigationIcon = {
//                    if (navigateUp != null) {
//                        NavigateUpButton(navigateUp)
//                    }
//                },
//            )
//        },
//        bottomBar = {
//            BottomAppBar {
//                Row(
//                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
//                    horizontalArrangement = Arrangement.SpaceEvenly,
//                ) {
//                    if (availableCredentials.isNotEmpty()) {
//                        RefreshDataButton(
//                            onClick = refreshData
//                        )
//                    } else {
//                        LoadDataButton(
//                            onClick = refreshData
//                        )
//                    }
//                }
//            }
//        }
//    ) { scaffoldPadding ->
//        Column(
//            modifier = Modifier.padding(scaffoldPadding)
//        ) {
//            Column(
//                modifier = Modifier.padding(horizontal = 16.dp)
//                    .verticalScroll(rememberScrollState())
//            ) {
//                val columnSpacingModifier = Modifier.padding(top = 16.dp)
//                Column {
//                    Text(
//                        stringResource(Res.string.info_text_redirection_to_id_austria_for_credential_provisioning),
//                    )
//                }
//                Column(
//                    modifier = columnSpacingModifier,
//                ) {
//                    val listSpacingModifier = Modifier.padding(top = 8.dp)
//                    Text(
//                        text = stringResource(Res.string.section_heading_configuration),
//                        style = MaterialTheme.typography.titleMedium,
//                    )
//
//                    IssuingServiceInputField(
//                        value = host,
//                        onValueChange = onChangeHost,
//                        modifier = listSpacingModifier.fillMaxWidth(),
//                    )
//                    StatefulCredentialRepresentationInputField(
//                        value = credentialRepresentation,
//                        onValueChange = onChangeCredentialRepresentation,
//                        modifier = listSpacingModifier.fillMaxWidth(),
//                    )
//                    StatefulCredentialSchemeInputField(
//                        value = credentialScheme,
//                        onValueChange = onChangeCredentialScheme,
//                        modifier = listSpacingModifier.fillMaxWidth(),
//                    )
//                }
//                Column(
//                    modifier = columnSpacingModifier,
//                ) {
//                    if (availableAttributesCategorized.values.flatten().isNotEmpty()) {
//                        Text("Bestehende Daten")
//                        availableAttributesCategorized.filter {
//                            it.value.isNotEmpty()
//                        }.forEach { attributeCategory ->
//                            val categoryAttributes = attributeCategory.value
//                            CategorySelectionRow(
//                                label = stringResource(attributeCategory.key.categoryTitle),
//                                checked = categoryAttributes.all { requestedAttributes.contains(it) },
//                                onCheckedChange = {
//                                    if (it) {
//                                        onChangeRequestedAttributes(requestedAttributes + categoryAttributes)
//                                    } else {
//                                        onChangeRequestedAttributes(requestedAttributes - categoryAttributes)
//                                    }
//                                },
//                                isExpanded = availableAttributeCategoryExpanded[attributeCategory.key]
//                                    ?: true,
//                                onToggleExpanded = {
//                                    availableAttributeCategoryExpanded =
//                                        availableAttributeCategoryExpanded + Pair(
//                                            attributeCategory.key,
//                                            !(availableAttributeCategoryExpanded[attributeCategory.key]
//                                                ?: true)
//                                        )
//                                },
//                                attributeSelections = attributeCategory.value.map {
//                                    AttributeSelection(
//                                        attributeLabel = it.attributeTranslation?.let {
//                                            stringResource(
//                                                it
//                                            )
//                                        } ?: it,
//                                        isSelected = requestedAttributes.contains(it)
//                                    )
//                                },
//                                onToggleAttributeSelection = { index ->
//                                    val attribute = categoryAttributes[index]
//                                    if (requestedAttributes.contains(attribute)) {
//                                        onChangeRequestedAttributes(requestedAttributes - attribute)
//                                    } else {
//                                        onChangeRequestedAttributes(requestedAttributes + attribute)
//                                    }
//                                }
//                            )
//                        }
//                    }
//                    if (missingAttributesCategorized.values.flatten().isNotEmpty()) {
//                        Text("Neue Daten")
//                        missingAttributesCategorized.filter {
//                            it.value.isNotEmpty()
//                        }.forEach { attributeCategory ->
//                            val categoryAttributes = attributeCategory.value
//                            CategorySelectionRow(
//                                label = stringResource(attributeCategory.key.categoryTitle),
//                                checked = categoryAttributes.all { requestedAttributes.contains(it) },
//                                onCheckedChange = {
//                                    if (it) {
//                                        onChangeRequestedAttributes(requestedAttributes + categoryAttributes)
//                                    } else {
//                                        onChangeRequestedAttributes(requestedAttributes - categoryAttributes)
//                                    }
//                                },
//                                isExpanded = missingAttributeCategoryExpanded[attributeCategory.key]
//                                    ?: true,
//                                onToggleExpanded = {
//                                    missingAttributeCategoryExpanded =
//                                        missingAttributeCategoryExpanded + Pair(
//                                            attributeCategory.key,
//                                            !(missingAttributeCategoryExpanded[attributeCategory.key]
//                                                ?: true)
//                                        )
//                                },
//                                attributeSelections = attributeCategory.value.map {
//                                    AttributeSelection(
//                                        attributeLabel = it.attributeTranslation?.let {
//                                            stringResource(
//                                                it
//                                            )
//                                        } ?: it,
//                                        isSelected = requestedAttributes.contains(it)
//                                    )
//                                },
//                                onToggleAttributeSelection = { index ->
//                                    onChangeRequestedAttributes(requestedAttributes - categoryAttributes[index])
//                                }
//                            )
//                        }
//                    }
//                }
//            }
//        }
////        items(
////            count = credentialScheme.claimNames.size,
////            key = { index -> credentialScheme.claimNames.toList()[index] }
////        ) { index ->
////            DataCategoryDisplaySection(
////                title = "Neue Daten laden",
////                attributes = listOf(
////
////                )
////            )
////            LabeledCheckbox(
////                label = credentialScheme.claimNames.toList()[index].let {
////                    it.attributeTranslation?.let { translation ->
////                        stringResource(translation)
////                    } ?: it
////                },
////                checked = requestedAttributes.contains(credentialScheme.claimNames.toList()[index]),
////                onCheckedChange = {
////                    onChangeRequestedAttributes(
////                        if (it) {
////                            requestedAttributes + credentialScheme.claimNames.toList()[index]
////                        } else {
////                            requestedAttributes - credentialScheme.claimNames.toList()[index]
////                        }
////                    )
////                }
////            )
////        }
//    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
private fun LoadDataView(
    // state
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
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    StatefulCredentialSchemeInputField(
                        value = credentialScheme,
                        onValueChange = onChangeCredentialScheme,
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                }
                if (availableAttributesCategorized.values.flatten().isNotEmpty()) {
                    Column(
                        modifier = columnSpacingModifier,
                    ) {
                        Text("Bestehende Daten")
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Row(
                                modifier = CategorySelectionRowDefaults.modifier,
                            ) {
                                LabeledCheckbox(
                                    label = "Alle Bestehende Daten",
                                    checked = availableAttributesCategorized.values.flatten()
                                        .all { requestedAttributes.contains(it) },
                                    onCheckedChange = {
                                        if (it) {
                                            onChangeRequestedAttributes(
                                                requestedAttributes + availableAttributesCategorized.values.flatten()
                                            )
                                        } else {
                                            onChangeRequestedAttributes(
                                                requestedAttributes - availableAttributesCategorized.values.flatten()
                                                    .toSet()
                                            )
                                        }
                                    },
                                    gapWidth = 16.dp,
                                    labelTextStyle = MaterialTheme.typography.bodyLarge,
                                )
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
                                requestedAttributes = requestedAttributes,
                                onChangeRequestedAttributes = onChangeRequestedAttributes,
                            )
                        }
                    }
                }
                if (missingAttributesCategorized.values.flatten().isNotEmpty()) {
                    Column(
                        modifier = columnSpacingModifier,
                    ) {
                        Text("Neue Daten")
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Row(
                                modifier = CategorySelectionRowDefaults.modifier,
                            ) {
                                LabeledCheckbox(
                                    label = "Alle Neue Daten",
                                    checked = missingAttributesCategorized.values.flatten()
                                        .all { requestedAttributes.contains(it) },
                                    onCheckedChange = {
                                        if (it) {
                                            onChangeRequestedAttributes(
                                                requestedAttributes + missingAttributesCategorized.values.flatten()
                                            )
                                        } else {
                                            onChangeRequestedAttributes(
                                                requestedAttributes - missingAttributesCategorized.values.flatten()
                                                    .toSet()
                                            )
                                        }
                                    },
                                    gapWidth = 16.dp,
                                    labelTextStyle = MaterialTheme.typography.bodyLarge,
                                )
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


//data class LoadDataCategoryUiState(
//    val categoryName: String,
//    val attributes: List<String>,
//    val isSelected: Boolean = false,
//    val isExpanded: Boolean = false,
//)

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun LoadDataView(
//    navigateUp: () -> Unit,
//    newDataItems: List<LoadDataCategoryUiState> = listOf(),
//    setNewDataItemSelection: (index: Int, newValue: Boolean) -> Unit,
//    setAllNewDataItemSelections: (newValue: Boolean) -> Unit,
//    setNewDataItemExpanded: (index: Int, newValue: Boolean) -> Unit,
//    existingDataItems: List<LoadDataCategoryUiState> = listOf(),
//    setExistingDataItemSelection: (index: Int, newValue: Boolean) -> Unit,
//    setAllExistingDataItemSelections: (newValue: Boolean) -> Unit,
//    setExistingDataItemExpanded: (index: Int, newValue: Boolean) -> Unit,
//    loadData: () -> Unit, // the indices of the items
//) {
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        text = "Daten Laden",
//                        style = MaterialTheme.typography.headlineLarge,
//                    )
//                },
//                navigationIcon = {
//                    IconButton(
//                        onClick = navigateUp,
//                    ) {
//                        Icon(
//                            imageVector = Icons.Filled.ArrowBack,
//                            contentDescription = "Navigate Back",
//                        )
//                    }
//                }
//            )
//        },
//        bottomBar = {
//            BottomAppBar {
//                Row(
//                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.Center,
//                ) {
//                    TextIconButton(
//                        icon = {
//                            Icon(
//                                imageVector = Icons.Default.ArrowForward,
//                                contentDescription = "Weiter",
//                            )
//                        },
//                        text = {
//                            Text(
//                                "Weiter",
//                                textAlign = TextAlign.Center,
//                            )
//                        },
//                        onClick = loadData,
//                    )
//                }
//            }
//        }
//    ) {
//        Box(modifier = Modifier.padding(it).verticalScroll(rememberScrollState())) {
//            Column(
//                modifier = Modifier.padding(16.dp)
//            ) {
//                Text("Welche Daten möchten Sie in diese App laden?")
//                Spacer(modifier = Modifier.height(32.dp))
//                if (newDataItems.isNotEmpty()) {
//                    Column {
//                        if (existingDataItems.isNotEmpty()) {
//                            Text(
//                                text = "Neue Daten laden",
//                                style = MaterialTheme.typography.titleMedium,
//                            )
//                            Spacer(modifier = Modifier.height(8.dp))
//                        }
//                        Column {
//                            Row(
//                                modifier = DataSelectionRowDefaults.modifier,
//                            ) {
//                                LabeledCheckbox(
//                                    label = if (existingDataItems.isNotEmpty()) "Alle neuen Daten" else "Alle Daten",
//                                    checked = newDataItems.all { it.isSelected },
//                                    onCheckedChange = {
//                                        setAllNewDataItemSelections(!newDataItems.all { it.isSelected })
//                                    },
//                                )
//                            }
//                            for (itemIndex in newDataItems.indices) {
//                                val item = newDataItems[itemIndex]
//                                DataSelectionRow(
//                                    label = item.categoryName,
//                                    checked = item.isSelected,
//                                    onCheckedChange = {
//                                        setNewDataItemSelection(itemIndex, !item.isSelected)
//                                    },
//                                    isExpanded = item.isExpanded,
//                                    onToggleExpanded = {
//                                        setNewDataItemExpanded(itemIndex, !item.isExpanded)
//                                    },
//                                    details = item.attributes,
//                                )
//                            }
//                        }
//                    }
//                }
//                if (existingDataItems.isNotEmpty()) {
//                    Column {
//                        if (newDataItems.isNotEmpty()) {
//                            Text(
//                                text = "Bestehende Daten aktualisieren",
//                                style = MaterialTheme.typography.titleMedium,
//                            )
//                            Spacer(modifier = Modifier.height(8.dp))
//                        }
//                        Column {
//                            Row(
//                                modifier = DataSelectionRowDefaults.modifier,
//                            ) {
//                                LabeledCheckbox(
//                                    label = if (newDataItems.isNotEmpty()) "Alle bestehenden Daten" else "Alle Daten",
//                                    checked = existingDataItems.all { it.isSelected },
//                                    onCheckedChange = {
//                                        setAllExistingDataItemSelections(!existingDataItems.all { it.isSelected })
//                                    },
//                                )
//                            }
//                            for (itemIndex in existingDataItems.indices) {
//                                val item = existingDataItems[itemIndex]
//                                DataSelectionRow(
//                                    label = item.categoryName,
//                                    checked = item.isSelected,
//                                    onCheckedChange = {
//                                        setExistingDataItemSelection(itemIndex, !item.isSelected)
//                                    },
//                                    isExpanded = item.isExpanded,
//                                    onToggleExpanded = {
//                                        setExistingDataItemExpanded(itemIndex, !item.isExpanded)
//                                    },
//                                    details = item.attributes,
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CategorySelectionRow(
    attributeCategory: Map.Entry<PersonalDataCategory, List<String>>,
    isExpanded: Boolean,
    onToggleExpanded: (Boolean) -> Unit,
    requestedAttributes: Set<String>,
    onChangeRequestedAttributes: (Set<String>) -> Unit,
) {
    val categoryAttributes = attributeCategory.value
    CategorySelectionRow(
        label = stringResource(attributeCategory.key.categoryTitle),
        checked = categoryAttributes.all { requestedAttributes.contains(it) },
        onCheckedChange = {
            if (it) {
                onChangeRequestedAttributes(requestedAttributes + categoryAttributes)
            } else {
                onChangeRequestedAttributes(requestedAttributes - categoryAttributes)
            }
        },
        isExpanded = isExpanded,
        onToggleExpanded = {
            val oldValue = isExpanded
            onToggleExpanded(!oldValue)
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
        }
    )
}

@Composable
fun CategorySelectionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isExpanded: Boolean = false,
    onToggleExpanded: () -> Unit,
    attributeSelections: List<AttributeSelection>,
    onToggleAttributeSelection: (index: Int) -> Unit,
    modifier: Modifier = CategorySelectionRowDefaults.modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LabeledCheckbox(
            label = label,
            checked = checked,
            onCheckedChange = onCheckedChange,
            gapWidth = 16.dp,
            labelTextStyle = MaterialTheme.typography.bodyLarge,
        )
        ExpandButton(
            isExpanded = isExpanded,
            onClick = onToggleExpanded,
            contentDescription = if (isExpanded) {
                "Zugehörige Attribute verbergen"
            } else {
                "Zugehörige Attribute anzeigen"
            }
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
//package ui.views
//
//import ExpandButton
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.ArrowForward
//import androidx.compose.material3.BottomAppBar
//import androidx.compose.material3.Divider
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.material3.TopAppBar
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import ui.composables.LabeledCheckbox
//import ui.composables.TextIconButton
//import ui.views.DataSelectionRowDefaults.Companion.withDefaultDataSelectionRowModifier

//data class LoadData2CategoryUiState(
//    val categoryName: String,
//    val attributes: List<String>,
//    val isSelected: Boolean = false,
//    val isExpanded: Boolean = false,
//)
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun LoadDataView2(
//    newDataItems: List<LoadDataCategoryUiState> = listOf(),
//    toggleNewDataItemSelection: (index: Int) -> Unit,
//    toggleAllNewDataItemSelections: () -> Unit,
//    toggleNewDataItemExpanded: (index: Int) -> Unit,
//    existingDataItems: List<LoadDataCategoryUiState> = listOf(),
//    toggleExistingDataItemSelection: (index: Int) -> Unit,
//    toggleAllExistingDataItemSelections: () -> Unit,
//    toggleExistingDataItemExpanded: (index: Int) -> Unit,
//    loadData: () -> Unit,
//    navigateUp: (() -> Unit)? = null,
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
//                    if (navigateUp != null) {
//                        IconButton(
//                            onClick = navigateUp,
//                        ) {
//                            Icon(
//                                imageVector = Icons.Filled.ArrowBack,
//                                contentDescription = "Navigate Back",
//                            )
//                        }
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
//    ) { scaffoldPadding ->
//        Box(modifier = Modifier.padding(scaffoldPadding).verticalScroll(rememberScrollState())) {
//            Column(
//                modifier = Modifier.padding(16.dp)
//            ) {
//                Text("Welche Daten möchten Sie in diese App laden?")
//                Spacer(modifier = Modifier.height(32.dp))
//                if (newDataItems.isNotEmpty()) {
//                    LoadDataCategorySection(
//                        title = if (existingDataItems.isNotEmpty()) "Neue Daten laden" else null,
//                        selectAllCheckboxLabel = if (existingDataItems.isNotEmpty()) "Alle neuen Daten" else "Alle Daten",
//                        dataItems = newDataItems,
//                        toggleAllDataItemSelections = toggleAllNewDataItemSelections,
//                        toggleDataItemSelection = toggleNewDataItemSelection,
//                        toggleDataItemExpanded = toggleNewDataItemExpanded,
//                    )
//                }
//                if (existingDataItems.isNotEmpty()) {
//                    LoadDataCategorySection(
//                        title = "Bestehende Daten aktualisieren",
//                        selectAllCheckboxLabel = if (newDataItems.isNotEmpty()) "Alle bestehenden Daten" else "Alle Daten",
//                        dataItems = existingDataItems,
//                        toggleAllDataItemSelections = toggleAllExistingDataItemSelections,
//                        toggleDataItemSelection = toggleExistingDataItemSelection,
//                        toggleDataItemExpanded = toggleExistingDataItemExpanded,
//                    )
//                }
//            }
//        }
//    }
//}
//
//
//@Composable
//fun LoadDataCategorySection(
//    title: String?,
//    selectAllCheckboxLabel: String,
//    dataItems: List<LoadDataCategoryUiState> = listOf(),
//    toggleDataItemSelection: (index: Int) -> Unit,
//    toggleAllDataItemSelections: () -> Unit,
//    toggleDataItemExpanded: (index: Int) -> Unit,
//) {
//    Column {
//        if(title != null) {
//            Text(
//                text = title,
//                style = MaterialTheme.typography.titleMedium,
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//        }
//        Column {
//            Row(
//                modifier = Modifier
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(color = MaterialTheme.colorScheme.primaryContainer)
//                    .withDefaultDataSelectionRowModifier()
//            ) {
//                LabeledCheckbox(
//                    label = selectAllCheckboxLabel,
//                    checked = dataItems.all { it.isSelected },
//                    onCheckedChange = {
//                        toggleAllDataItemSelections()
//                    },
//                )
//            }
//            for (itemIndex in dataItems.indices) {
//                val item = dataItems[itemIndex]
//                DataSelectionRow(
//                    label = item.categoryName,
//                    checked = item.isSelected,
//                    onCheckedChange = {
//                        toggleDataItemSelection(itemIndex)
//                    },
//                    isExpanded = item.isExpanded,
//                    onToggleExpanded = {
//                        toggleDataItemExpanded(itemIndex)
//                    },
//                    details = item.attributes,
//                )
//            }
//        }
//    }
//}
//
//
//class DataSelectionRowDefaults {
//    companion object {
//        val modifier = Modifier.fillMaxWidth().padding(ModifierDefaults.paddingValues)
//
//        fun Modifier.withDefaultDataSelectionRowModifier() : Modifier {
//            return this.fillMaxWidth().padding(ModifierDefaults.paddingValues)
//        }
//
//        class ModifierDefaults {
//            companion object {
//                val paddingValues = PaddingValues(
//                    top = 8.dp,
//                    end = 24.dp,
//                    bottom = 8.dp,
//                    start = 16.dp,
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun DataSelectionRow(
//    label: String,
//    checked: Boolean,
//    onCheckedChange: () -> Unit,
//    isExpanded: Boolean = false,
//    onToggleExpanded: () -> Unit,
//    details: List<String>,
//    modifier: Modifier = DataSelectionRowDefaults.modifier,
//) {
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.SpaceBetween,
//        modifier = modifier,
//    ) {
//        LabeledCheckbox(
//            label = label,
//            checked = checked,
//            onCheckedChange = onCheckedChange,
//            gapWidth = 16.dp,
//            labelTextStyle = MaterialTheme.typography.bodyLarge,
//        )
//        ExpandButton(
//            isExpanded = isExpanded,
//            onClick = onToggleExpanded,
//            contentDescription = if (isExpanded) {
//                "Zugehörige Attribute verbergen"
//            } else {
//                "Zugehörige Attribute anzeigen"
//            }
//        )
//    }
//    if (isExpanded) {
//        Column(
//            modifier = Modifier.fillMaxWidth()
//                .padding(horizontal = 24.dp)
//        ) {
//            Divider(modifier = Modifier.fillMaxWidth())
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(
//                        top = 8.dp,
//                        end = 0.dp,
//                        bottom = 8.dp,
//                        start = 48.dp, // deviation from the figma file
//                    )
//            ) {
//                val bullet = "\u2022"
//                // source: https://stackoverflow.com/questions/70724196/how-to-create-bulleted-text-list-in-android-jetpack-compose
//                details.forEach {
//                    Row {
//                        Text(
//                            text = bullet,
//                            style = MaterialTheme.typography.bodyMedium,
//                        )
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text(
//                            text = it,
//                            style = MaterialTheme.typography.bodyMedium,
//                        )
//                    }
//                }
//            }
//            Divider(modifier = Modifier.fillMaxWidth())
//        }
//    }
//}

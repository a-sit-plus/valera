package ui.views

import ExpandButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.composables.LabeledCheckbox
import ui.composables.OutlinedTextIconButton
import ui.composables.TextIconButton

data class LoadDataCategoryUiState(
    val categoryName: String,
    val attributes: List<String>,
    val isSelected: Boolean = false,
    val isExpanded: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadDataView(
    navigateUp: () -> Unit,
    newDataItems: List<LoadDataCategoryUiState> = listOf(),
    setNewDataItemSelection: (index: Int, newValue: Boolean) -> Unit,
    setAllNewDataItemSelections: (newValue: Boolean) -> Unit,
    setNewDataItemExpanded: (index: Int, newValue: Boolean) -> Unit,
    existingDataItems: List<LoadDataCategoryUiState> = listOf(),
    setExistingDataItemSelection: (index: Int, newValue: Boolean) -> Unit,
    setAllExistingDataItemSelections: (newValue: Boolean) -> Unit,
    setExistingDataItemExpanded: (index: Int, newValue: Boolean) -> Unit,
    loadData: () -> Unit, // the indices of the items
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Daten Laden",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateUp,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Navigate Back",
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextIconButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Weiter",
                            )
                        },
                        text = {
                            Text(
                                "Weiter",
                                textAlign = TextAlign.Center,
                            )
                        },
                        onClick = loadData,
                    )
                }
            }
        }
    ) {
        val dataSelectionRowModifier = Modifier.fillMaxWidth().padding(
            top = 8.dp,
            end = 24.dp,
            bottom = 8.dp,
            start = 16.dp,
        )

        Box(modifier = Modifier.padding(it).verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Welche Daten möchten Sie in diese App laden?")
                Spacer(modifier = Modifier.height(32.dp))
                if (newDataItems.isNotEmpty()) {
                    Column {
                        if (existingDataItems.isNotEmpty()) {
                            Text(
                                text = "Neue Daten laden",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Column {
                            Row(
                                modifier = DataSelectionRowDefaults.modifier,
                            ) {
                                LabeledCheckbox(
                                    label = if (existingDataItems.isNotEmpty()) "Alle neuen Daten" else "Alle Daten",
                                    checked = newDataItems.all { it.isSelected },
                                    onCheckedChange = {
                                        setAllNewDataItemSelections(!newDataItems.all { it.isSelected })
                                    },
                                )
                            }
                            for (itemIndex in newDataItems.indices) {
                                val item = newDataItems[itemIndex]
                                DataSelectionRow(
                                    label = item.categoryName,
                                    checked = item.isSelected,
                                    onCheckedChange = {
                                        setNewDataItemSelection(itemIndex, !item.isSelected)
                                    },
                                    isExpanded = item.isExpanded,
                                    onToggleExpanded = {
                                        setNewDataItemExpanded(itemIndex, !item.isExpanded)
                                    },
                                    details = item.attributes,
                                )
//                                Row(
//                                    modifier = DataSelectionRowDefaults.modifier,
//                                    verticalAlignment = Alignment.CenterVertically,
//                                    horizontalArrangement = Arrangement.SpaceBetween,
//                                ) {
//                                    LabeledCheckbox(
//                                        label = item.categoryName,
//                                        checked = false,
//                                        onCheckedChange = {}
//                                    )
//                                    IconButton(
//                                        onClick = {
//
//                                        }, // TODO: expand section
//                                    ) {
//                                        Icon(
//                                            imageVector = Icons.Default.ExpandMore,
//                                            contentDescription = "Zugehörige Attribute anzeigen"
//                                        )
//                                    }
//                                }
//                                if(item.isExpanded) {
//
//                                }
                            }
                        }
                    }
                }
                if (existingDataItems.isNotEmpty()) {
                    Column {
                        if (newDataItems.isNotEmpty()) {
                            Text(
                                text = "Bestehende Daten aktualisieren",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Column {
                            Row(
                                modifier = DataSelectionRowDefaults.modifier,
                            ) {
                                LabeledCheckbox(
                                    label = if (newDataItems.isNotEmpty()) "Alle bestehenden Daten" else "Alle Daten",
                                    checked = existingDataItems.all { it.isSelected },
                                    onCheckedChange = {
                                        setAllExistingDataItemSelections(!existingDataItems.all { it.isSelected })
                                    },
                                )
                            }
                            for (itemIndex in existingDataItems.indices) {
                                val item = existingDataItems[itemIndex]
                                DataSelectionRow(
                                    label = item.categoryName,
                                    checked = item.isSelected,
                                    onCheckedChange = {
                                        setExistingDataItemSelection(itemIndex, !item.isSelected)
                                    },
                                    isExpanded = item.isExpanded,
                                    onToggleExpanded = {
                                        setExistingDataItemExpanded(itemIndex, !item.isExpanded)
                                    },
                                    details = item.attributes,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


class DataSelectionRowDefaults {
    companion object {
        val modifier = Modifier.fillMaxWidth().padding(
            top = 8.dp,
            end = 24.dp,
            bottom = 8.dp,
            start = 16.dp,
        )
    }
}

@Composable
fun DataSelectionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    isExpanded: Boolean = false,
    onToggleExpanded: () -> Unit,
    details: List<String>,
    modifier: Modifier = DataSelectionRowDefaults.modifier,
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
            Divider(modifier = Modifier.fillMaxWidth())
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 8.dp, end = 0.dp, bottom = 8.dp, start = 34.dp)
            ) {
                // source: https://stackoverflow.com/questions/70724196/how-to-create-bulleted-text-list-in-android-jetpack-compose
                details.forEach {
                    Row {
                        Text(
                            text = "\u2022 $it",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
//            Text(
//                buildAnnotatedString {
//                    details.forEach {
//                        withStyle(style = paragraphStyle) {
//                            append(bullet)
//                            append(it)
//                        }
//                    }
//                },
//            )
            Divider(modifier = Modifier.fillMaxWidth())
        }
    }
}

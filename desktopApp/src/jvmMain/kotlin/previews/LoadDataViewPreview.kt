package previews

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import ui.views.LoadDataView
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.views.LoadDataCategoryUiState

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        var newDataCategories = rememberSaveable {
            mutableStateListOf(
                LoadDataCategoryUiState(
                    categoryName = "Identitätsdaten",
                    attributes = listOf("Vorname, Nachname", "Geburtsdatum", "Bereichsspezifisches Personenkennzeichen", "Aktuelles Foto aus zentralem Identitätsdokumentenregister"),
                    isSelected = true,
                    isExpanded = true,
                ),
                LoadDataCategoryUiState(
                    categoryName = "Zulassungsdaten",
                    attributes = listOf("Klasse A", "Klasse B"),
                    isSelected = false,
                    isExpanded = false,
                ),
            )
        }
        var existingDataCategories = rememberSaveable {
            mutableStateListOf(
                LoadDataCategoryUiState(
                    categoryName = "Identitätsdaten",
                    attributes = listOf("Vorname, Nachname", "Geburtsdatum", "Bereichsspezifisches Personenkennzeichen", "Aktuelles Foto aus zentralem Identitätsdokumentenregister"),
                    isSelected = false,
                    isExpanded = false,
                ),
                LoadDataCategoryUiState(
                    categoryName = "Zulassungsdaten",
                    attributes = listOf("Klasse A", "Klasse B"),
                    isSelected = true,
                    isExpanded = true,
                ),
            )
        }
        LoadDataView(
            navigateUp = {},
            newDataItems = newDataCategories,
            setNewDataItemExpanded = { index, newValue ->
                newDataCategories[index] = newDataCategories[index].copy(isExpanded = newValue)
            },
            setNewDataItemSelection = {index, newValue ->
                newDataCategories[index] = newDataCategories[index].copy(isSelected = newValue)
            },
            setAllNewDataItemSelections = { newValue ->
                newDataCategories.indices.forEach { index ->
                    newDataCategories[index] = newDataCategories[index].copy(isSelected = newValue)
                }
            },
            existingDataItems = existingDataCategories,
            setExistingDataItemExpanded = { index, newValue ->
                existingDataCategories[index] = existingDataCategories[index].copy(isExpanded = newValue)
            },
            setExistingDataItemSelection = {index, newValue ->
                existingDataCategories[index] = existingDataCategories[index].copy(isSelected = newValue)
            },
            setAllExistingDataItemSelections = { newValue ->
                existingDataCategories.indices.forEach { index ->
                    existingDataCategories[index] = existingDataCategories[index].copy(isSelected = newValue)
                }
            },
            loadData = {}
        )
    }
}

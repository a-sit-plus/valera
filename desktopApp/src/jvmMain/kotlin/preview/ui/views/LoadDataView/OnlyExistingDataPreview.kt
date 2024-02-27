package preview.ui.views.LoadDataView

//import ui.views.LoadDataCategoryUiState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.views.LoadDataView

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
//        val newDataCategories = rememberSaveable {
//            mutableStateListOf<LoadDataCategoryUiState>()
//        }
//        val existingDataCategories = rememberSaveable {
//            mutableStateListOf(
//                LoadDataCategoryUiState(
//                    categoryName = "Identitätsdaten",
//                    attributes = listOf("Vorname, Nachname", "Geburtsdatum", "Bereichsspezifisches Personenkennzeichen", "Aktuelles Foto aus zentralem Identitätsdokumentenregister"),
//                    isSelected = true,
//                    isExpanded = true,
//                ),
//                LoadDataCategoryUiState(
//                    categoryName = "Zulassungsdaten",
//                    attributes = listOf("Klasse A", "Klasse B"),
//                    isSelected = false,
//                    isExpanded = false,
//                ),
//            )
//        }
        LoadDataView(
            navigateUp = {},
//            newDataItems = newDataCategories,
//            toggleNewDataItemExpanded = { index ->
//                newDataCategories[index] = newDataCategories[index].copy(isExpanded = !newDataCategories[index].isExpanded)
//            },
//            toggleNewDataItemSelection = {index ->
//                newDataCategories[index] = newDataCategories[index].copy(isSelected = !newDataCategories[index].isSelected)
//            },
//            toggleAllNewDataItemSelections = {
//                val newValue = !newDataCategories.all { it.isSelected }
//                newDataCategories.indices.forEach { index ->
//                    newDataCategories[index] = newDataCategories[index].copy(isSelected = newValue)
//                }
//            },
//            existingDataItems = existingDataCategories,
//            toggleExistingDataItemExpanded = { index ->
//                existingDataCategories[index] = existingDataCategories[index].copy(isExpanded = !existingDataCategories[index].isExpanded)
//            },
//            toggleExistingDataItemSelection = {index ->
//                existingDataCategories[index] = existingDataCategories[index].copy(isSelected = !existingDataCategories[index].isSelected)
//            },
//            toggleAllExistingDataItemSelections = {
//                val newValue = !existingDataCategories.all { it.isSelected }
//                existingDataCategories.indices.forEach { index ->
//                    existingDataCategories[index] = existingDataCategories[index].copy(isSelected = newValue)
//                }
//            },
            loadData = {},
            navigateToQrCodeCredentialProvisioningPage = {},
        )
    }
}

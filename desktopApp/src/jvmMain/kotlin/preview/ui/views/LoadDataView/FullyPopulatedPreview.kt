package preview.ui.views.LoadDataView

//import ui.views.LoadDataCategoryUiState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.views.LoadDataView

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        LoadDataView(
            navigateUp = {},
            loadData = {},
            navigateToQrCodeCredentialProvisioningPage = {},
        )
    }
}

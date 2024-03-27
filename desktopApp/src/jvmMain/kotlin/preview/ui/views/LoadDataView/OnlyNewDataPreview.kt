package preview.ui.views.LoadDataView

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.views.StatefulLoadDataView

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        StatefulLoadDataView(
            navigateUp = {},
            loadData = {},
        )
    }
}

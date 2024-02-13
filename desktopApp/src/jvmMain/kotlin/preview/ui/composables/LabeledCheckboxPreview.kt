package preview.ui.composables

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.composables.LabeledCheckbox

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        val checked = rememberSaveable {
            mutableStateOf(false)
        }
        LabeledCheckbox(
            label = "Test",
            checked = checked.value,
            onCheckedChange = {
                checked.value = !checked.value
            }
        )
    }
}


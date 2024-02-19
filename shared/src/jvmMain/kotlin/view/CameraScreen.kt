package view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CameraView(
    onFoundPayload: (text: String) -> Unit,
    modifier: Modifier,
) {
    Text("Actual CameraView")
}

package view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun CameraView(onFoundPayload: (text: String) -> Unit) {
    Text("Actual CameraView")
}

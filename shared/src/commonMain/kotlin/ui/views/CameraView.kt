package ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

fun interface CameraView {
    @Composable
    operator fun invoke(
        onFoundPayload: (text: String) -> Unit,
        modifier: Modifier,
    )
}
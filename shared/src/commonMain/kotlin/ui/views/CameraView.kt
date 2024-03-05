package ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
expect fun CameraView(
    onFoundPayload: (text: String) -> Unit,
    modifier: Modifier = Modifier,
)
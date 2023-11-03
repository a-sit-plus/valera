package view

import androidx.compose.runtime.Composable

@Composable
expect fun CameraView(onFoundPayload: (text: String) -> Unit)


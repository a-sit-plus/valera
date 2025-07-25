package ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

val lightExtendedColors = ExtendedColors(
    success = Color(0xFF006D3A),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFA6F4C0),
    onSuccessContainer = Color(0xFF00210D),
)

val darkExtendedColors = ExtendedColors(
    success = Color(0xFF81DB9E),
    onSuccess = Color(0xFF00391F),
    successContainer = Color(0xFF005231),
    onSuccessContainer = Color(0xFFA6F4C0),
)

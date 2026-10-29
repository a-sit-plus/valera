package ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val validationLight: ValidationColor,
    val validationDark: ValidationColor,
)

val lightExtendedColors = ExtendedColors(
    success = Color(0xFF006D3A),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFA6F4C0),
    onSuccessContainer = Color(0xFF00210D),
    validationLight = ValidationColor(
        valid = Color(red = 235, green = 255, blue = 235),
        invalid = Color(red = 255, green = 235, blue = 235),
        warning = Color(red = 255, green = 245, blue = 220),
    ),
    validationDark = ValidationColor(
        valid = Color(red = 0, green = 150, blue = 0),
        invalid = Color(red = 150, green = 0, blue = 0),
        warning = Color(red = 160, green = 100, blue = 0),
    ),
)

val darkExtendedColors = ExtendedColors(
    success = Color(0xFF81DB9E),
    onSuccess = Color(0xFF00391F),
    successContainer = Color(0xFF005231),
    onSuccessContainer = Color(0xFFA6F4C0),

    validationLight = ValidationColor(
        valid = Color(red = 0, green = 150, blue = 0),
        invalid = Color(red = 150, green = 0, blue = 0),
        warning = Color(red = 160, green = 100, blue = 0),
    ),
    validationDark = ValidationColor(
        valid = Color(red = 235, green = 255, blue = 235),
        invalid = Color(red = 255, green = 235, blue = 235),
        warning = Color(red = 255, green = 225, blue = 160),
    ),
)

data class ValidationColor(
    val valid: Color,
    val invalid: Color,
    /** Of a check that could not be checked, as opposed to one that failed. */
    val warning: Color,
)

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
    /** Yellow, e.g. for states that could not be determined. */
    val caution: Color,
    val onCaution: Color,
    val cautionContainer: Color,
    val onCautionContainer: Color,
)

val lightExtendedColors = ExtendedColors(
    success = Color(0xFF006D3A),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFA6F4C0),
    onSuccessContainer = Color(0xFF00210D),
    validationLight = ValidationColor(
        valid = Color(red = 235, green = 255, blue = 235),
        invalid = Color(red = 255, green = 235, blue = 235),
    ),
    validationDark = ValidationColor(
        valid = Color(red = 0, green = 150, blue = 0),
        invalid = Color(red = 150, green = 0, blue = 0)
    ),
    caution = Color(0xFF6E5E00),
    onCaution = Color(0xFFFFFFFF),
    cautionContainer = Color(0xFFFFE170),
    onCautionContainer = Color(0xFF221B00),
)

val darkExtendedColors = ExtendedColors(
    success = Color(0xFF81DB9E),
    onSuccess = Color(0xFF00391F),
    successContainer = Color(0xFF005231),
    onSuccessContainer = Color(0xFFA6F4C0),
    validationLight = ValidationColor(
        valid = Color(red = 0, green = 150, blue = 0),
        invalid = Color(red = 150, green = 0, blue = 0),
    ),
    validationDark = ValidationColor(
        valid = Color(red = 235, green = 255, blue = 235),
        invalid = Color(red = 255, green = 235, blue = 235)
    ),
    caution = Color(0xFFE5C400),
    onCaution = Color(0xFF3A3000),
    cautionContainer = Color(0xFF544600),
    onCautionContainer = Color(0xFFFFE170),
)

data class ValidationColor(
    val valid: Color,
    val invalid: Color,
)

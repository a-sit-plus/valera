package ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import getColorScheme


// Modified from https://github.com/android/codelab-android-compose/tree/main/BasicsCodelab
@Composable
fun WalletTheme(
    content: @Composable () -> Unit
) {
    val colors = getColorScheme()

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
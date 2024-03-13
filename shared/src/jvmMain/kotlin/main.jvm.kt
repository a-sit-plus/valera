
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter
import ui.theme.darkScheme
import ui.theme.lightScheme

actual fun getPlatformName(): String = "Desktop"

// Modified from https://developer.android.com/jetpack/compose/designsystems/material3
@Composable
actual fun getColorScheme(): ColorScheme {
    // Dynamic color is available on Android 12+
    val darkColorScheme = darkScheme
    val lightColorScheme = lightScheme
    val darkTheme = isSystemInDarkTheme()
    return when {
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }
}

@Composable fun MainView(objectFactory: ObjectFactory, platformAdapter: PlatformAdapter) {
    Text("Desktop not supported")
}
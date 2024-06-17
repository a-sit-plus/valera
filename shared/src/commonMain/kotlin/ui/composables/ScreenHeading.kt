package ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ScreenHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
    )
}
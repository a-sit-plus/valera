package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

@Composable
fun BackNavigationButton(
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = Resources.CONTENT_DESCRIPTION_NAVIGATE_BACK,
        )
    }
}
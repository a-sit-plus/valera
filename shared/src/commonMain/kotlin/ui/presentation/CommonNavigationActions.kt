package ui.presentation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.composables.Logo

@Composable
fun RowScope.CommonNavigationActions(
    onClickLogo: () -> Unit,
) {
    Logo(onClick = onClickLogo)
    Spacer(Modifier.Companion.width(15.dp))
}

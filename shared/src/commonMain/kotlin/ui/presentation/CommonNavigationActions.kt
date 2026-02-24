package ui.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.composables.Logo

@Composable
fun RowScope.CommonNavigationActions(
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
) {
    Logo(onClick = onClickLogo)
    Column(modifier = Modifier.Companion.clickable(onClick = onClickSettings)) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = null,
        )
    }
    Spacer(Modifier.Companion.width(15.dp))
}

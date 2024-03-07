package ui.composables

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import containerHeight
import padding

@Composable
fun FloatingActionButtonHeightSpacer(
    externalPadding: Dp = 0.dp,
) {
    Spacer(modifier = Modifier.height(FloatingActionButtonDefaults.padding + FloatingActionButtonDefaults.containerHeight + FloatingActionButtonDefaults.padding - externalPadding))
}
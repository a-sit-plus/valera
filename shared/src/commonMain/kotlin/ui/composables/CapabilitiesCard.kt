package ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_capabilities_settings
import org.jetbrains.compose.resources.stringResource

@Composable
fun CapabilityCard(text: String, success: Boolean, info: String, action: (() -> Unit)? = null) {
    val expanded = mutableStateOf(!success)

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(),
        modifier = Modifier.clickable(onClick = { expanded.value = !expanded.value }, enabled = !success)
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row {
                    Text(
                        when (success) {
                            true -> "✅"
                            false -> "❌"
                        }
                    )
                    Spacer(Modifier.width(20.dp))
                    Text(text, fontWeight = FontWeight.Bold)
                }
                if (!success) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier
                    )
                }
            }
            val density = LocalDensity.current
            AnimatedVisibility(
                visible = expanded.value,
                enter = slideInVertically {
                    with(density) { -20.dp.roundToPx() }
                } + expandVertically(
                    expandFrom = Alignment.Top
                ) + fadeIn(
                    initialAlpha = 0.3f
                ),
                exit = slideOutVertically {
                    with(density) { 20.dp.roundToPx() }
                } + shrinkVertically(
                    shrinkTowards = Alignment.Bottom
                ) + fadeOut(
                    targetAlpha = 0f
                )
            ) {
                Column(modifier = Modifier.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(info)
                    action?.let {
                        OutlinedButton(onClick = it) {
                            Text(stringResource(Res.string.button_label_capabilities_settings))
                        }
                    }
                }
            }

        }
    }
}
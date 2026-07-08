package ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ExpandableCard(
    text: String,
    icon: ImageVector,
    expanded: Boolean,
    lightColor: Color? = null,
    darkColor: Color? = null,
    content: @Composable () -> Unit
) {
    val expanded = remember { mutableStateOf(expanded) }
    val arrowIcon = when (expanded.value) {
        true -> Icons.Outlined.ArrowUpward
        else -> Icons.Outlined.ArrowDownward
    }
    val radius = 8.dp

    val shape = when (expanded.value) {
        true -> RoundedCornerShape(radius, radius, 0.dp, 0.dp)
        else -> RoundedCornerShape(radius)
    }
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Column(
            modifier = Modifier
                .clip(shape = shape)
                .background(color = lightColor ?: Color.Unspecified)
                .clickable(onClick = { expanded.value = !expanded.value })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = Dp.Hairline,
                        color = darkColor ?: MaterialTheme.colorScheme.onSurface,
                        shape = shape
                    )
                    .padding(horizontal = 12.dp)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = darkColor ?: MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = text, color = darkColor ?: Color.Unspecified, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Icon(
                        imageVector = arrowIcon,
                        contentDescription = null,
                        tint = darkColor ?: LocalContentColor.current,
                    )
                }
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
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(0.dp, 0.dp, radius, radius))
                    .border(
                        width = Dp.Hairline,
                        color = darkColor ?: MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(0.dp, 0.dp, radius, radius)
                    )
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
            ) {
                content()
            }
        }

    }
}
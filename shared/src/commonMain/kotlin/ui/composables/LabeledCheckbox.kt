package ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LabeledCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    gapWidth: Dp = LabeledCheckboxDefaults.gapWidth,
    labelTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier,
) {
    LabeledCheckboxLayout(
        checkbox = {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
        text = {
            Text(
                text = label,
                style = labelTextStyle,
            )
        },
        gapWidth = gapWidth,
        modifier = modifier,
    )
}

@Composable
fun LabeledTriStateCheckbox(
    label: String,
    state: ToggleableState,
    onClick: () -> Unit,
    enabled: Boolean = true,
    gapWidth: Dp = LabeledCheckboxDefaults.gapWidth,
    labelTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier,
) {
    LabeledCheckboxLayout(
        checkbox = {
            TriStateCheckbox(
                state = state,
                onClick = onClick,
                enabled = enabled,
            )
        },
        text = {
            Text(
                text = label,
                style = labelTextStyle,
            )
        },
        gapWidth = gapWidth,
        modifier = modifier
    )
}

@Composable
fun LabeledCheckboxLayout(
    checkbox: @Composable () -> Unit,
    text: @Composable () -> Unit,
    gapWidth: Dp = LabeledCheckboxDefaults.gapWidth,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        checkbox()
        Spacer(modifier = Modifier.width(gapWidth))
        text()
    }
}

class LabeledCheckboxDefaults {
    companion object {
        val gapWidth: Dp = 16.dp
    }
}
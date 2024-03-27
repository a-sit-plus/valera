package ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class LabeledCheckboxDefaults {
    companion object {
        val gapWidth: Dp = 16.dp
    }
}

@Composable
fun LabeledCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    gapWidth: Dp = LabeledCheckboxDefaults.gapWidth,
    labelTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = {
                onCheckedChange(it)
            },
        )
        Spacer(modifier = Modifier.width(gapWidth))
        Text(
            text = label,
            style = labelTextStyle,
        )
    }
}
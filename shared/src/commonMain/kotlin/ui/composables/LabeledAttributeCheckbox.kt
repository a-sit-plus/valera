package ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier

@Composable
fun LabeledTextCheckbox(
    label: String,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
    ){
    val checked = mutableStateOf(checked)

    val onCheckedChange = { bool: Boolean ->
        checked.value = bool
        onCheckedChange(checked.value)
    }

    Row(modifier = Modifier.clickable(onClick = {
        checked.value = !checked.value
        onCheckedChange(checked.value)
    }).fillMaxWidth()) {
        Checkbox(
            checked = checked.value,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
        LabeledText(label = label, text = text)
    }
}
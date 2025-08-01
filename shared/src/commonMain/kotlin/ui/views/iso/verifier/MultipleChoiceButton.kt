package ui.views.iso.verifier

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun MultipleChoiceButton(
    current: String,
    value: Boolean,
    contains: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.toggleable(
            value = value,
            onValueChange = onValueChange,
            role = Role.Checkbox
        )
    ) {
        Checkbox(checked = contains, onCheckedChange = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = current)
    }
}

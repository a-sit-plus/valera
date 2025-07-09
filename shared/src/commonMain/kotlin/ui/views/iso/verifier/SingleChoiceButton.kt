package ui.views.iso.verifier

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun SingleChoiceButton(
    current: String,
    selectedOption: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    onOptionSelected: () -> Unit,
) {
    Row(
        modifier = modifier.selectable(
            selected = (current == selectedOption),
            onClick = onOptionSelected,
            role = Role.RadioButton
        )
    ) {
        RadioButton(selected = (current == selectedOption), onClick = null)
        icon?.invoke()
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = current)
    }
}

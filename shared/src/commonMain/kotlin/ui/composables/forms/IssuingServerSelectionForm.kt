package ui.composables.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ui.composables.inputFields.IssuingServiceInputField


@Composable
fun IssuingServerSelectionForm(
    host: TextFieldValue,
    onChangeHost: ((TextFieldValue) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        val listSpacingModifier = Modifier.padding(top = 8.dp)
        IssuingServiceInputField(
            value = host,
            onValueChange = onChangeHost,
            modifier = listSpacingModifier.fillMaxWidth(),
        )
    }
}
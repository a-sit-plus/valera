package ui.composables.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.section_heading_configuration
import org.jetbrains.compose.resources.stringResource
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
        Text(
            text = stringResource(Res.string.section_heading_configuration),
            style = MaterialTheme.typography.titleMedium,
        )

        IssuingServiceInputField(
            value = host,
            onValueChange = onChangeHost,
            modifier = listSpacingModifier.fillMaxWidth(),
        )
    }
}
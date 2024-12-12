package ui.composables.inputFields

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.issuing_label_transaction_code
import org.jetbrains.compose.resources.stringResource

@Composable
fun TransactionCodeInputField(
    value: TextFieldValue,
    onValueChange: ((TextFieldValue) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    TransactionCodeInputField(
        value = value,
        onValueChange = onValueChange ?: {},
        enabled = onValueChange != null,
        modifier = modifier,
    )
}

@Composable
fun TransactionCodeInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(stringResource(Res.string.issuing_label_transaction_code))
        },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done
        ),
        modifier = modifier,
    )
}
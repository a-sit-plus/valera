package ui.composables.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.*
import at.asitplus.wallet.app.common.validation.IssuerValidationUi
import at.asitplus.wallet.lib.oidvci.CredentialIdentifierInfo
import org.jetbrains.compose.resources.stringResource
import ui.composables.inputFields.CredentialIdentifierSelection
import ui.composables.inputFields.TransactionCodeInputField


@Composable
fun CredentialMetadataSelectionForm(
    host: String,
    credentialIdentifierInfos: List<CredentialIdentifierInfo>,
    onChangeCredentialIdentifierInfos: (List<CredentialIdentifierInfo>) -> Unit,
    transactionCode: TextFieldValue,
    onChangeTransactionCode: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    availableIdentifiers: Collection<CredentialIdentifierInfo>,
    showTransactionCode: Boolean,
    issuer: IssuerValidationUi? = null,
) {
    Column(
        modifier = modifier,
    ) {
        val listSpacingModifier = Modifier.padding(top = 8.dp)
        Row(modifier = modifier) {
            Text(
                text = stringResource(Res.string.issuing_label_host),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(": ")
            Text(
                text = host,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(
            text = stringResource(Res.string.issuing_label_credentials),
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier,
        )
        CredentialIdentifierSelection(
            selected = credentialIdentifierInfos,
            onSelectedChange = onChangeCredentialIdentifierInfos,
            availableIdentifiers = availableIdentifiers,
            modifier = listSpacingModifier.fillMaxWidth(),
            issuer = issuer,
        )
        Row(modifier = modifier) {
            Text(
                text = stringResource(Res.string.issuing_label_information),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (showTransactionCode) {
            TransactionCodeInputField(
                value = transactionCode,
                onValueChange = onChangeTransactionCode,
                modifier = listSpacingModifier.fillMaxWidth(),
            )
        }
    }
}

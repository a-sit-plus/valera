package ui.composables.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.*
import at.asitplus.wallet.app.common.resolveCredentialScheme
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import at.asitplus.wallet.lib.oidvci.toRepresentation
import org.jetbrains.compose.resources.stringResource
import ui.composables.inputFields.StatefulCredentialIdentifierInputField
import ui.composables.inputFields.TransactionCodeInputField


@Composable
fun CredentialMetadataSelectionForm(
    host: String,
    credentialIdentifierInfo: CredentialIdentifierInfo,
    onChangeCredentialIdentifierInfo: (CredentialIdentifierInfo) -> Unit,
    transactionCode: TextFieldValue,
    onChangeTransactionCode: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    availableIdentifiers: Collection<CredentialIdentifierInfo>,
    showTransactionCode: Boolean,
) {
    val scheme by produceState<CredentialScheme?>(null, credentialIdentifierInfo) {
        value = credentialIdentifierInfo.supportedCredentialFormat.resolveCredentialScheme()
    }
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
        StatefulCredentialIdentifierInputField(
            value = credentialIdentifierInfo,
            onValueChange = onChangeCredentialIdentifierInfo,
            modifier = listSpacingModifier.fillMaxWidth(),
            availableIdentifiers = availableIdentifiers
        )
        Row(modifier = modifier) {
            Text(
                text = stringResource(Res.string.issuing_label_scheme),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(": ")
            Text(
                text = scheme.uiLabel(),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Row(modifier = modifier) {
            Text(
                text = stringResource(Res.string.issuing_label_representation),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(": ")
            Text(
                text = credentialIdentifierInfo.supportedCredentialFormat.format.toRepresentation().uiLabel(),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
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
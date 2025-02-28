package ui.composables.forms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_redirection_to_browser_for_credential_provisioning
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatefulLoadDataForm(
    host: String,
    credentialIdentifierInfo: CredentialIdentifierInfo,
    onChangeCredentialIdentifierInfo: (CredentialIdentifierInfo) -> Unit,
    transactionCode: TextFieldValue,
    onChangeTransactionCode: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    availableIdentifiers: Collection<CredentialIdentifierInfo>,
    showTransactionCode: Boolean,
) {

    LoadDataForm(
        host = host,
        credentialIdentifierInfo = credentialIdentifierInfo,
        onChangeCredentialIdentifierInfo = onChangeCredentialIdentifierInfo,
        transactionCode = transactionCode,
        onChangeTransactionCode = onChangeTransactionCode,
        modifier = modifier,
        availableIdentifiers = availableIdentifiers,
        showTransactionCode = showTransactionCode,
    )
}

@Composable
fun LoadDataForm(
    host: String,
    credentialIdentifierInfo: CredentialIdentifierInfo,
    onChangeCredentialIdentifierInfo: (CredentialIdentifierInfo) -> Unit,
    transactionCode: TextFieldValue,
    onChangeTransactionCode: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    availableIdentifiers: Collection<CredentialIdentifierInfo>,
    showTransactionCode: Boolean,
) {
    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
        ) {
            val columnSpacingModifier = Modifier.padding(top = 16.dp)
            Text(stringResource(Res.string.info_text_redirection_to_browser_for_credential_provisioning))
            CredentialMetadataSelectionForm(
                host = host,
                credentialIdentifierInfo = credentialIdentifierInfo,
                onChangeCredentialIdentifierInfo = onChangeCredentialIdentifierInfo,
                modifier = columnSpacingModifier,
                availableIdentifiers = availableIdentifiers,
                transactionCode = transactionCode,
                onChangeTransactionCode = onChangeTransactionCode,
                showTransactionCode = showTransactionCode,
            )
        }
    }
}

package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import ui.composables.buttons.LoadDataButton
import ui.composables.forms.StatefulLoadDataForm

@Composable
fun LoadDataView(
    // state
    host: String,
    credentialIdentifierInfo: CredentialIdentifierInfo,
    onChangeCredentialIdentifierInfo: (CredentialIdentifierInfo) -> Unit,
    transactionCode: TextFieldValue,
    onChangeTransactionCode: (TextFieldValue) -> Unit,
    // other
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    availableIdentifiers: Collection<CredentialIdentifierInfo>,
    showTransactionCode: Boolean,
) {
    Scaffold(
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    LoadDataButton(
                        onClick = onSubmit
                    )
                }
            }
        },
        modifier = modifier,
    ) { scaffoldPadding ->
        StatefulLoadDataForm(
            host = host,
            credentialIdentifierInfo = credentialIdentifierInfo,
            onChangeCredentialIdentifierInfo = onChangeCredentialIdentifierInfo,
            transactionCode = transactionCode,
            onChangeTransactionCode = onChangeTransactionCode,
            modifier = Modifier.padding(scaffoldPadding),
            availableIdentifiers = availableIdentifiers,
            showTransactionCode = showTransactionCode
        )
    }
}

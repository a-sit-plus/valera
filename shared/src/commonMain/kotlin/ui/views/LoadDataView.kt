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
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.CredentialOfferGrantsPreAuthCodeTransactionCode
import at.asitplus.wallet.lib.data.ConstantIndex
import ui.composables.buttons.LoadDataButton
import ui.composables.forms.StatefulLoadDataForm

@Composable
fun LoadDataView(
    // state
    host: TextFieldValue,
    transactionCode: TextFieldValue,
    onChangeHost: ((TextFieldValue) -> Unit),
    credentialRepresentation: ConstantIndex.CredentialRepresentation,
    onChangeCredentialRepresentation: ((ConstantIndex.CredentialRepresentation) -> Unit),
    credentialScheme: ConstantIndex.CredentialScheme,
    onChangeCredentialScheme: ((ConstantIndex.CredentialScheme) -> Unit),
    requestedAttributes: Set<NormalizedJsonPath>,
    onChangeRequestedAttributes: ((Set<NormalizedJsonPath>) -> Unit),
    onChangeTransactionCode: (TextFieldValue) -> Unit,
    // other
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    showAttributes: Boolean,
    transactionCodeInfo: CredentialOfferGrantsPreAuthCodeTransactionCode?,
    availableSchemeRepresentations: Map<ConstantIndex.CredentialScheme, Collection<ConstantIndex.CredentialRepresentation>>,
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
            transactionCode = transactionCode,
            onChangeHost = onChangeHost,
            credentialRepresentation = credentialRepresentation,
            onChangeCredentialRepresentation = onChangeCredentialRepresentation,
            credentialScheme = credentialScheme,
            onChangeCredentialScheme = onChangeCredentialScheme,
            requestedAttributes = requestedAttributes,
            onChangeRequestedAttributes = onChangeRequestedAttributes,
            onChangeTransactionCode = onChangeTransactionCode,
            modifier = Modifier.padding(scaffoldPadding),
            showAttributes = showAttributes,
            transactionCodeInfo = transactionCodeInfo,
            availableSchemeRepresentations = availableSchemeRepresentations
        )
    }
}

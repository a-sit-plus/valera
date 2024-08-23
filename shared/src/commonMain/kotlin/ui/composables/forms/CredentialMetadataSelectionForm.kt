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
import at.asitplus.wallet.lib.data.ConstantIndex
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.section_heading_configuration
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.inputFields.IssuingServiceInputField
import ui.composables.inputFields.StatefulCredentialRepresentationInputField
import ui.composables.inputFields.StatefulCredentialSchemeInputField


@Composable
fun CredentialMetadataSelectionForm(
    host: TextFieldValue,
    onChangeHost: ((TextFieldValue) -> Unit)?,
    credentialRepresentation: ConstantIndex.CredentialRepresentation,
    onChangeCredentialRepresentation: ((ConstantIndex.CredentialRepresentation) -> Unit)?,
    credentialScheme: ConstantIndex.CredentialScheme,
    onChangeCredentialScheme: ((ConstantIndex.CredentialScheme) -> Unit)?,
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
        StatefulCredentialSchemeInputField(
            value = credentialScheme,
            onValueChange = onChangeCredentialScheme,
            modifier = listSpacingModifier.fillMaxWidth(),
        )
        StatefulCredentialRepresentationInputField(
            value = credentialRepresentation,
            onValueChange = onChangeCredentialRepresentation,
            options = credentialScheme.supportedRepresentations.toList(),
            modifier = listSpacingModifier.fillMaxWidth(),
        )
    }
}
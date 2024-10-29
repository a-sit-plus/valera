package ui.composables.forms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.identifier
import at.asitplus.wallet.lib.data.ConstantIndex
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.info_text_redirection_to_browser_for_credential_provisioning
import data.PersonalDataCategory
import data.credentials.CredentialAttributeCategorization
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatefulSelectIssuingServerForm(
    host: TextFieldValue,
    onChangeHost: ((TextFieldValue) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    SelectIssuingServerForm(
        host = host,
        onChangeHost = onChangeHost,
        modifier = modifier,
    )
}

@Composable
fun SelectIssuingServerForm(
    host: TextFieldValue,
    onChangeHost: ((TextFieldValue) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
        ) {
            val columnSpacingModifier = Modifier.padding(top = 16.dp)
            Text(
                stringResource(Res.string.info_text_redirection_to_browser_for_credential_provisioning),
            )

            IssuingServerSelectionForm(
                host = host,
                onChangeHost = onChangeHost,
                modifier = columnSpacingModifier,
            )
        }
    }
}
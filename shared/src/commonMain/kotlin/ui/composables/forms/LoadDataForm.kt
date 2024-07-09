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
import at.asitplus.wallet.lib.data.ConstantIndex
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.info_text_redirection_to_id_austria_for_credential_provisioning
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import data.PersonalDataCategory
import data.attributeCategorizationOrder

@Composable
fun StatefulLoadDataForm(
    host: TextFieldValue,
    onChangeHost: ((TextFieldValue) -> Unit)?,
    credentialRepresentation: ConstantIndex.CredentialRepresentation,
    onChangeCredentialRepresentation: ((ConstantIndex.CredentialRepresentation) -> Unit)?,
    credentialScheme: ConstantIndex.CredentialScheme,
    onChangeCredentialScheme: ((ConstantIndex.CredentialScheme) -> Unit)?,
    requestedAttributes: Set<String>,
    onChangeRequestedAttributes: ((Set<String>) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var attributeCategoriesExpanded by rememberSaveable(credentialScheme) {
        mutableStateOf(attributeCategorizationOrder.associateWith {
            false
        })
    }

    LoadDataForm(
        host = host,
        onChangeHost = onChangeHost,
        credentialRepresentation = credentialRepresentation,
        onChangeCredentialRepresentation = onChangeCredentialRepresentation,
        credentialScheme = credentialScheme,
        onChangeCredentialScheme = onChangeCredentialScheme,
        requestedAttributes = requestedAttributes,
        onChangeRequestedAttributes = onChangeRequestedAttributes,
        attributeCategoriesExpanded = attributeCategoriesExpanded,
        onSetAttributeCategoriesExpanded = {
            attributeCategoriesExpanded += it
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun LoadDataForm(
    host: TextFieldValue,
    onChangeHost: ((TextFieldValue) -> Unit)?,
    credentialRepresentation: ConstantIndex.CredentialRepresentation,
    onChangeCredentialRepresentation: ((ConstantIndex.CredentialRepresentation) -> Unit)?,
    credentialScheme: ConstantIndex.CredentialScheme,
    onChangeCredentialScheme: ((ConstantIndex.CredentialScheme) -> Unit)?,
    requestedAttributes: Set<String>,
    onChangeRequestedAttributes: ((Set<String>) -> Unit)?,
    attributeCategoriesExpanded: Map<PersonalDataCategory, Boolean>,
    onSetAttributeCategoriesExpanded: (Pair<PersonalDataCategory, Boolean>) -> Unit,
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
                stringResource(Res.string.info_text_redirection_to_id_austria_for_credential_provisioning),
            )

            CredentialMetadataSelectionForm(
                host = host,
                onChangeHost = onChangeHost,
                credentialRepresentation = credentialRepresentation,
                onChangeCredentialRepresentation = onChangeCredentialRepresentation,
                credentialScheme = credentialScheme,
                onChangeCredentialScheme = onChangeCredentialScheme,
                modifier = columnSpacingModifier,
            )

            CredentialAttributeSelectionForm(
                credentialScheme = credentialScheme,
                requestedAttributes = requestedAttributes,
                onChangeRequestedAttributes = onChangeRequestedAttributes,
                attributeCategoriesExpanded = attributeCategoriesExpanded,
                onSetAttributeCategoriesExpanded = onSetAttributeCategoriesExpanded,
                modifier = columnSpacingModifier,
            )
        }
    }
}
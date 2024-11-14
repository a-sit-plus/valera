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
import at.asitplus.wallet.app.common.CredentialIdentifierInfo
import at.asitplus.wallet.app.common.credentialScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.info_text_redirection_to_browser_for_credential_provisioning
import data.PersonalDataCategory
import data.credentials.CredentialAttributeCategorization
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatefulLoadDataForm(
    host: String,
    credentialIdentifierInfo: CredentialIdentifierInfo,
    onChangeCredentialIdentifierInfo: (CredentialIdentifierInfo) -> Unit,
    requestedAttributes: Set<NormalizedJsonPath>,
    onChangeRequestedAttributes: ((Set<NormalizedJsonPath>) -> Unit)?,
    transactionCode: TextFieldValue,
    onChangeTransactionCode: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    availableIdentifiers: Collection<CredentialIdentifierInfo>,
    showTransactionCode: Boolean,
) {
    var attributeCategoriesExpanded by rememberSaveable(credentialIdentifierInfo) {
        mutableStateOf(CredentialAttributeCategorization[credentialIdentifierInfo.credentialScheme].availableCategories.associateWith { false })
    }

    LoadDataForm(
        host = host,
        credentialIdentifierInfo = credentialIdentifierInfo,
        onChangeCredentialIdentifierInfo = onChangeCredentialIdentifierInfo,
        requestedAttributes = requestedAttributes,
        onChangeRequestedAttributes = onChangeRequestedAttributes,
        transactionCode = transactionCode,
        onChangeTransactionCode = onChangeTransactionCode,
        attributeCategoriesExpanded = attributeCategoriesExpanded,
        onSetAttributeCategoriesExpanded = { attributeCategoriesExpanded += it },
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
    requestedAttributes: Set<NormalizedJsonPath>,
    onChangeRequestedAttributes: ((Set<NormalizedJsonPath>) -> Unit)?,
    transactionCode: TextFieldValue,
    onChangeTransactionCode: (TextFieldValue) -> Unit,
    attributeCategoriesExpanded: Map<PersonalDataCategory, Boolean>,
    onSetAttributeCategoriesExpanded: (Pair<PersonalDataCategory, Boolean>) -> Unit,
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
            CredentialAttributeSelectionForm(
                credentialScheme = credentialIdentifierInfo.credentialScheme ?: ConstantIndex.AtomicAttribute2023,
                requestedAttributes = requestedAttributes,
                onChangeRequestedAttributes = onChangeRequestedAttributes,
                attributeCategoriesExpanded = attributeCategoriesExpanded,
                onSetAttributeCategoriesExpanded = onSetAttributeCategoriesExpanded,
                modifier = columnSpacingModifier,
            )
        }
    }
}
package ui.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.data.NonEmptyList.Companion.toNonEmptyList
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_add_credential_screen
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.LoadCredentialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadCredentialView(
    vm: LoadCredentialViewModel
) {
    // The selected credentials by their identifier, in the order of the metadata: none at first, as the user selects
    // every credential to request explicitly
    var selectedIdentifiers by rememberSaveable(stateSaver = stringListSaver) { mutableStateOf(emptyList<String>()) }
    val credentialIdentifierInfos = vm.credentialIdentifiers.filter { it.credentialIdentifier in selectedIdentifiers }

    var transactionCode by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.heading_label_add_credential_screen),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                actions = {
                    Logo(onClick = vm.onClickLogo)
                    Spacer(Modifier.width(15.dp))
                },
                navigationIcon = {
                    NavigateUpButton(vm.navigateUp)
                },
            )
        },
    ) { scaffoldPadding ->
        LoadDataView(
            host = vm.hostString,
            credentialIdentifierInfos = credentialIdentifierInfos,
            onChangeCredentialIdentifierInfos = { selected ->
                selectedIdentifiers = selected.map { it.credentialIdentifier }
            },
            transactionCode = transactionCode,
            onChangeTransactionCode = { transactionCode = it },
            onSubmit = {
                val selection = credentialIdentifierInfos.takeIf { it.isNotEmpty() }?.toNonEmptyList()
                if (!isSubmitting && selection != null) {
                    isSubmitting = true
                    try {
                        vm.onSubmit(selection, transactionCode.text, vm.offer)
                    } catch (e: Throwable) {
                        isSubmitting = false
                        throw e
                    }
                }
            },
            modifier = Modifier.padding(scaffoldPadding),
            availableIdentifiers = vm.credentialIdentifiers,
            showTransactionCode = vm.offer?.grants?.preAuthorizedCode?.transactionCode != null,
            isSubmitEnabled = !isSubmitting && credentialIdentifierInfos.isNotEmpty(),
            issuer = vm.issuer,
        )
    }
}

private val stringListSaver = listSaver<List<String>, String>(save = { it }, restore = { it })

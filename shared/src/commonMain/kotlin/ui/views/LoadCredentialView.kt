package ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.asp
import at.asitplus.valera.resources.heading_label_add_credential_screen
import at.asitplus.valera.resources.heading_label_error_screen
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.state.savers.CredentialIdentifierInfoSaver
import ui.state.savers.asMutableStateSaver
import ui.viewmodels.LoadCredentialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadCredentialView(
    vm: LoadCredentialViewModel
) {

    val vm = remember { vm }

    var credentialIdentifierInfo by rememberSaveable(saver = CredentialIdentifierInfoSaver().asMutableStateSaver()) {
        mutableStateOf(vm.credentialIdentifiers.first())
    }

    var requestedAttributes by rememberSaveable(credentialIdentifierInfo) {
        runBlocking {
            mutableStateOf(setOf<NormalizedJsonPath>())
        }
    }

    var transactionCode by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.heading_label_add_credential_screen),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Image(
                            modifier = Modifier.padding(start = 0.dp, end = 8.dp, top = 8.dp),
                            painter = painterResource(Res.drawable.asp),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                        )
                    }
                },
                navigationIcon = {
                    NavigateUpButton(vm.navigateUp)
                },
            )
        },
    ) { scaffoldPadding ->
        LoadDataView(
            host = vm.hostString,
            credentialIdentifierInfo = credentialIdentifierInfo,
            onChangeCredentialIdentifierInfo = { credentialIdentifierInfo = it },
            requestedAttributes = requestedAttributes,
            onChangeRequestedAttributes = { requestedAttributes = it },
            transactionCode = transactionCode,
            onChangeTransactionCode = { transactionCode = it },
            onSubmit = { vm.onSubmit(credentialIdentifierInfo, requestedAttributes, transactionCode.text) },
            modifier = Modifier.padding(scaffoldPadding),
            availableIdentifiers = runBlocking { vm.credentialIdentifiers },
            showTransactionCode = vm.transactionCodeRequirements != null,
        )
    }
}
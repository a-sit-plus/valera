package ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.asp
import at.asitplus.valera.resources.heading_label_add_credential_screen
import at.asitplus.valera.resources.heading_label_navigate_back
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.ScreenHeading
import ui.composables.buttons.LoadDataButton
import ui.composables.buttons.NavigateUpButton
import ui.composables.forms.StatefulSelectIssuingServerForm
import ui.viewmodels.AddCredentialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectIssuingServerView(
    vm: AddCredentialViewModel
) {
    var host by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        runBlocking {
            mutableStateOf(TextFieldValue(vm.hostString))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.heading_label_add_credential_screen),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Image(
                            modifier = Modifier.padding(start = 0.dp, end = 0.dp, top = 8.dp),
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
        Scaffold(
            bottomBar = {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        LoadDataButton(
                            onClick = { vm.onSubmitServer(host.text) }
                        )
                    }
                }
            },
            modifier = Modifier.padding(scaffoldPadding),
        ) { scaffoldPadding ->
            StatefulSelectIssuingServerForm(
                host = host,
                onChangeHost = { host = it },
                modifier = Modifier.padding(scaffoldPadding),
            )
        }
    }
}

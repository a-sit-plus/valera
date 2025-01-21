package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_continue
import at.asitplus.valera.resources.heading_label_sign_document
import at.asitplus.valera.resources.text_label_qtsp
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.TextIconButton
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.SigningQtspSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigningQtspSelectionView(
    vm: SigningQtspSelectionViewModel
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
                            text = stringResource(Res.string.heading_label_sign_document),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Logo()
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
                        TextIconButton(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                )
                            },
                            text = {
                                Text(stringResource(Res.string.button_label_continue))
                            },
                            onClick = { vm.onContinue(host.text) },
                            modifier = Modifier,
                        )
                    }
                }
            },
            modifier = Modifier.padding(scaffoldPadding),
        ) { scaffoldPadding ->
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = {
                        Text(stringResource(Res.string.text_label_qtsp))
                    },
                    enabled = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

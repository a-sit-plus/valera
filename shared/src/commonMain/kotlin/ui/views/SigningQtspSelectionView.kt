package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_sign
import at.asitplus.valera.resources.heading_label_sign_document
import at.asitplus.valera.resources.text_label_credential_id
import at.asitplus.valera.resources.text_label_delete_certificate
import at.asitplus.valera.resources.text_label_preload_certificate
import at.asitplus.valera.resources.text_label_qtsp
import at.asitplus.valera.resources.text_label_valid_from
import at.asitplus.valera.resources.text_label_valid_to
import at.asitplus.wallet.app.common.SigningConfig
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import ui.composables.LabeledText
import ui.composables.Logo
import ui.composables.TextIconButton
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.SigningQtspSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigningQtspSelectionView(
    vm: SigningQtspSelectionViewModel
) {
    val config = vm.walletMain.signingService.config
    val selection = mutableStateOf(config.current)
    val credentialInfo = mutableStateOf(config.getQtspByIdentifier(selection.value).credentialInfo)


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
                        Logo(onClick = vm.onClickLogo)
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
                                Text(stringResource(Res.string.button_label_sign))
                            },
                            onClick = { vm.onContinue() },
                            modifier = Modifier,
                        )
                    }
                }
            },
            modifier = Modifier.padding(scaffoldPadding),
        ) { scaffoldPadding ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var expanded by remember { mutableStateOf(false) }
                QtspSelectionField(
                    value = selection.value,
                    onValueChange = {
                        selection.value = it
                        expanded = !expanded
                        config.current = it
                        runBlocking { vm.walletMain.signingService.exportToDataStore() }
                                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    config = config
                )
                Spacer(modifier = Modifier.height(10.dp))

                if(config.getCurrent().allowPreload) {
                    Row {
                        Button(onClick = { runBlocking { vm.walletMain.signingService.preloadCertificate() } }, enabled = (credentialInfo.value == null)) {
                            Text(stringResource(Res.string.text_label_preload_certificate))
                        }
                        Button(onClick = {
                            config.getQtspByIdentifier(selection.value).credentialInfo = null
                            credentialInfo.value = null
                            runBlocking { vm.walletMain.signingService.exportToDataStore() } }
                            , enabled = (credentialInfo.value != null)) {
                            Text(stringResource(Res.string.text_label_delete_certificate))
                        }
                    }
                }

                if (credentialInfo.value != null) {
                    Column(modifier = Modifier.padding(start = 32.dp)) {
                        LabeledText(
                            label = stringResource(Res.string.text_label_credential_id),
                            text = "${credentialInfo.value?.credentialID}",
                            modifier = Modifier,
                        )
                        LabeledText(
                            label = stringResource(Res.string.text_label_valid_from),
                            text = "${credentialInfo.value?.certParameters?.validFrom}",
                            modifier = Modifier,
                        )
                        LabeledText(
                            label = stringResource(Res.string.text_label_valid_to),
                            text = "${credentialInfo.value?.certParameters?.validTo}",
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QtspSelectionField(
    value: String,
    onValueChange: (String) -> Unit,
    expanded: Boolean,
    enabled: Boolean = true,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    config: SigningConfig,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        OutlinedTextField(
            readOnly = true,
            value = config.qtsps.filter { it.identifier == value }.first().qtspBaseUrl,
            onValueChange = {},
            label = { Text(stringResource(Res.string.text_label_qtsp)) },
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (qtsp in config.qtsps) {
                DropdownMenuItem(
                    text = { Text(qtsp.qtspBaseUrl) },
                    onClick = { onValueChange(qtsp.identifier) },
                    enabled = enabled,
                )
            }
        }
    }
}
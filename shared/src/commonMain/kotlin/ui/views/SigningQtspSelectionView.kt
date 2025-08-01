package ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import at.asitplus.rqes.CredentialInfo
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_continue
import at.asitplus.valera.resources.heading_label_select_vda
import at.asitplus.valera.resources.text_label_no_certificate
import at.asitplus.valera.resources.text_label_preload_certificate
import at.asitplus.valera.resources.text_label_selected_certificate
import at.asitplus.valera.resources.text_label_vda
import at.asitplus.wallet.app.common.QtspConfig
import org.jetbrains.compose.resources.stringResource
import ui.composables.CertificateCard
import ui.composables.DataDisplaySection
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.SigningQtspSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigningQtspSelectionView(
    vm: SigningQtspSelectionViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.heading_label_select_vda),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                actions = {
                    Logo(onClick = vm.onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = vm.onClickSettings)) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    }
                    Spacer(Modifier.width(15.dp))
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
                        Button(
                            content = {
                                Text(stringResource(Res.string.button_label_continue))
                            },
                            onClick = { vm.onContinue(vm.signatureRequestParameters) },
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
                DataDisplaySection(title = stringResource(Res.string.text_label_vda)) {
                    QtspSelectionField(
                        value = vm.selection.value,
                        onValueChange = {
                            vm.onQtspChange(it)
                            expanded = !expanded
                        },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        qtspList = vm.qtspList
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (vm.allowPreload()) {
                    DataDisplaySection(title = stringResource(Res.string.text_label_selected_certificate)) {
                        CertificateInfoField(
                            vm.credentialInfo.value,
                            vm.onClickPreload,
                            onClickDelete = vm.onClickDelete
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
    qtspList: List<QtspConfig>,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        OutlinedTextField(
            singleLine = true,
            readOnly = true,
            value = qtspList.filter { it.identifier == value }.first().qtspBaseUrl,
            onValueChange = {},
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (qtsp in qtspList) {
                DropdownMenuItem(
                    text = { Text(qtsp.qtspBaseUrl) },
                    onClick = { onValueChange(qtsp.identifier) },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
fun CertificateInfoField(
    credentialInfo: CredentialInfo?,
    onClickPreload: () -> Unit,
    onClickDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (credentialInfo != null) {
            var isExpanded by rememberSaveable {
                mutableStateOf(false)
            }
            CertificateCard(credentialInfo, isExpanded, { isExpanded = it }, onClickDelete)
        } else {
            Text(stringResource(Res.string.text_label_no_certificate))
            OutlinedButton(onClick = onClickPreload) {
                Text(stringResource(Res.string.text_label_preload_certificate))
            }
        }
    }
}
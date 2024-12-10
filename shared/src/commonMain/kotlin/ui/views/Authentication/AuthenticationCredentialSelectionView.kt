package ui.views.Authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.decodeImage
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.button_label_continue
import compose_wallet_app.shared.generated.resources.heading_label_navigate_back
import compose_wallet_app.shared.generated.resources.prompt_select_credential
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.NavigateUpButton
import ui.composables.credentials.CredentialSelectionGroup
import ui.viewmodels.Authentication.AuthenticationCredentialSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationCredentialSelectionView(vm: AuthenticationCredentialSelectionViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.heading_label_navigate_back),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    NavigateUpButton({ vm.navigateUp() })
                },
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    BottomAppBarDefaults.ContainerElevation
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.prompt_select_credential),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Button(onClick = {
                            val selection = vm.selectedCredential.entries.associate {
                                val requestId = it.key
                                val credentials = it.value.value
                                requestId to credentials
                            }
                            vm.selectCredential(selection)
                        }) {
                            Text(stringResource(Res.string.button_label_continue))
                        }
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())
                    .padding(16.dp),
            ) {
                vm.requests.forEach { request ->
                    val selection = mutableStateOf(request.value.second.keys.first())
                    vm.selectedCredential[request.key] = selection
                    val matchingCredentials = request.value.second.keys
                    if (matchingCredentials.size > 1) {
                        Text(
                            text = request.value.first.credentialIdentifier,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        CredentialSelectionGroup(
                            selectedCredential = selection,
                            credentials = matchingCredentials,
                            imageDecoder = { byteArray ->
                                vm.walletMain.platformAdapter.decodeImage(byteArray)
                            })
                    }
                }
            }
        }
    }

}
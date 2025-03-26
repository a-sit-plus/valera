package ui.views.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_continue
import at.asitplus.valera.resources.heading_label_navigate_back
import at.asitplus.valera.resources.prompt_select_credential
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.composables.credentials.CredentialSelectionGroup
import ui.viewmodels.authentication.AuthenticationSelectionPresentationExchangeViewModel

@Composable
fun AuthenticationSelectionPresentationExchangeView(vm: AuthenticationSelectionPresentationExchangeViewModel) {
    val vm = remember { vm }

    val currentRequest = vm.iterableRequests[vm.requestIterator.value]

    AuthenticationSelectionViewScaffold(
        onClickLogo = vm.onClickLogo,
        onNavigateUp = vm.onBack,
        onNext = vm.onNext,
    ) {
        LinearProgressIndicator(
            progress = { ((1.0f / vm.requests.size) * (vm.requestIterator.value + 1)) },
            modifier = Modifier.fillMaxWidth(),
            drawStopIndicator = { }
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())
                .padding(16.dp),
        ) {
            val requestId = currentRequest.first
            val matchingCredentials = currentRequest.second

            Text(
                text = matchingCredentials.keys.first().scheme.uiLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )
            val attributeSelection =
                vm.attributeSelection[requestId] ?: throw Throwable("No selection with requestId")
            val credentialSelection =
                vm.credentialSelection[requestId] ?: throw Throwable("No selection with requestId")
            CredentialSelectionGroup(
                matchingCredentials = matchingCredentials,
                attributeSelection = attributeSelection,
                credentialSelection = credentialSelection,
                imageDecoder = { byteArray ->
                    vm.walletMain.platformAdapter.decodeImage(byteArray)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationSelectionViewScaffold(
    onClickLogo: () -> Unit,
    onNavigateUp: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.heading_label_navigate_back),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Logo(onClick = onClickLogo)
                    }
                },
                navigationIcon = {
                    NavigateUpButton(onClick = onNavigateUp)
                },
            )
        },
        bottomBar = {
            Surface(
                color = NavigationBarDefaults.containerColor,
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
                        Button(onClick = onNext) {
                            Text(stringResource(Res.string.button_label_continue))
                        }
                    }
                }
            }
        },
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(it)) {
            content()
        }
    }
}
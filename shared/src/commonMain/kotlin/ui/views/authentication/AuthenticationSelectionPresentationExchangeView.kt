package ui.views.authentication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_continue
import at.asitplus.valera.resources.error_credential_selection_error_invalid_request_id
import at.asitplus.valera.resources.error_no_requests
import at.asitplus.valera.resources.heading_label_navigate_back
import at.asitplus.valera.resources.prompt_select_credential
import at.asitplus.wallet.app.common.decodeImage
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.composables.credentials.CredentialSelectionGroup
import ui.models.toCredentialFreshnessSummaryModel
import ui.viewmodels.authentication.AuthenticationSelectionPresentationExchangeViewModel

@Composable
fun AuthenticationSelectionPresentationExchangeView(
    vm: AuthenticationSelectionPresentationExchangeViewModel,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onError: (Throwable) -> Unit,
) {
    val iterableRequests = vm.iterableRequests
    if (iterableRequests.isEmpty()) {
        onError(Throwable(stringResource(Res.string.error_no_requests)))
    } else {
        val currentRequest = vm.iterableRequests[vm.requestIterator.value]

        AuthenticationSelectionViewScaffold(
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
            onNavigateUp = vm.onBack,
            onNext = vm.onNext,
        ) {
            LinearProgressIndicator(
                progress = { ((1.0f / vm.requests.size) * (vm.requestIterator.value + 1)) },
                modifier = Modifier.fillMaxWidth(),
                drawStopIndicator = { },
            )
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())
                    .padding(16.dp),
            ) {
                val requestId = currentRequest.first
                val matchingCredentials = currentRequest.second

                val attributeSelection = vm.attributeSelection[requestId]
                    ?: return@Column onError(Throwable(stringResource(Res.string.error_credential_selection_error_invalid_request_id)))
                val credentialSelection = vm.credentialSelection[requestId]
                    ?: return@Column onError(Throwable(stringResource(Res.string.error_credential_selection_error_invalid_request_id)))

                CredentialSelectionGroup(
                    matchingCredentials = matchingCredentials,
                    attributeSelection = attributeSelection,
                    credentialSelection = credentialSelection,
                    imageDecoder = { vm.walletMain.platformAdapter.decodeImage(it) },
                    checkCredentialFreshness = {
                        vm.walletMain.checkCredentialFreshness(it).toCredentialFreshnessSummaryModel()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationSelectionViewScaffold(
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
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
                    }
                },
                actions = {
                    Logo(onClick = onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    }
                    Spacer(Modifier.width(15.dp))
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
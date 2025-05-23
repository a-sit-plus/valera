package ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_my_data_screen
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ui.composables.CustomFloatingActionMenu
import ui.composables.FloatingActionButtonHeightSpacer
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.credentials.CredentialCard
import ui.viewmodels.CredentialsViewModel
import ui.viewmodels.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialsView(
    navigateToAddCredentialsPage: () -> Unit,
    navigateToQrAddCredentialsPage: () -> Unit,
    navigateToCredentialDetailsPage: (Long) -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    walletMain: WalletMain = koinInject(),
    onError: (Throwable) -> Unit,
    vm: CredentialsViewModel = koinViewModel(),
    bottomBar: @Composable () -> Unit
) {
    val storeContainer by vm.storeContainer.collectAsState()

    val credentialStatusesState by produceState(
        CredentialStatusesState.Loading() as CredentialStatusesState,
        storeContainer
    ) {
        when (val delegate = storeContainer) {
            is UiState.Success ->  {
                val credentialsWithStatus = mutableMapOf<Long, TokenStatus?>()
                delegate.value.credentials.forEach { (id, credential) ->
                    credentialsWithStatus[id] = walletMain.checkRevocationStatus(credential)
                    value = CredentialStatusesState.Loading(credentialsWithStatus)
                }
                value = CredentialStatusesState.Success(credentialsWithStatus)
            }

            is UiState.Loading -> value = CredentialStatusesState.Loading()
            is UiState.Failure -> value = CredentialStatusesState.Success()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ScreenHeading(
                            stringResource(Res.string.heading_label_my_data_screen),
                            Modifier.weight(1f),
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
                }
            )
        },
        floatingActionButton = {
            when (val it = storeContainer) {
                is UiState.Success -> {
                    if (it.value.credentials.isNotEmpty()) {
                        CustomFloatingActionMenu(
                            addCredential = navigateToAddCredentialsPage,
                            addCredentialQr = navigateToQrAddCredentialsPage
                        )
                    }
                }

                else -> {}
            }
        },
        bottomBar = { bottomBar() }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize(),
        ) {
            when (val credentialsStatusDelegate = storeContainer) {
                is UiState.Success -> {
                    val credentials = credentialsStatusDelegate.value.credentials.sortedBy { (id, credential) ->
                        credentialStatusesState.credentialStatuses[id]?.value ?: 256.toUByte()
                    }
                    if (credentials.isEmpty()) {
                        NoDataLoadedView(navigateToAddCredentialsPage, navigateToQrAddCredentialsPage)
                    } else {
                        LazyColumn {
                            items(
                                credentials.size,
                                key = {
                                    credentials[it].first
                                }
                            ) { index ->
                                val storeEntry = credentials[index]
                                val storeEntryIdentifier = storeEntry.first
                                val credential = storeEntry.second

                                val isTokenStatusEvaluated =
                                    storeEntryIdentifier in credentialStatusesState.credentialStatuses
                                val tokenStatus = credentialStatusesState.credentialStatuses[storeEntryIdentifier]

                                Column {
                                    CredentialCard(
                                        credential,
                                        isTokenStatusEvaluated = isTokenStatusEvaluated,
                                        tokenStatus = tokenStatus,
                                        onDelete = {
                                            vm.removeStoreEntryById(storeEntryIdentifier)
                                        },
                                        onOpenDetails = {
                                            navigateToCredentialDetailsPage(storeEntryIdentifier)
                                        },
                                        imageDecoder = vm.imageDecoder::invoke,
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            bottom = 16.dp
                                        ),
                                    )
                                }
                            }
                            item {
                                FloatingActionButtonHeightSpacer(
                                    externalPadding = 16.dp
                                )
                            }
                        }
                    }
                }

                is UiState.Loading -> Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is UiState.Failure -> Box(modifier = Modifier.fillMaxSize()) {
                    LaunchedEffect(Unit) {
                        onError(credentialsStatusDelegate.throwable)
                    }
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

private sealed interface CredentialStatusesState {
    val credentialStatuses: Map<Long, TokenStatus?>

    data class Loading(
        override val credentialStatuses: Map<Long, TokenStatus?> = mapOf(),
    ) : CredentialStatusesState

    data class Success(
        override val credentialStatuses: Map<Long, TokenStatus?> = mapOf(),
    ) : CredentialStatusesState
}
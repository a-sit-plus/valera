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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.content_description_navigate_to_settings
import at.asitplus.valera.resources.heading_label_my_data_screen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope
import ui.composables.CustomFloatingActionMenu
import ui.composables.FloatingActionButtonHeightSpacer
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.credentials.CredentialCard
import ui.models.CredentialFreshnessSummaryUiModel
import ui.viewmodels.CredentialStateModel
import ui.viewmodels.CredentialsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialsView(
    navigateToAddCredentialsPage: () -> Unit,
    navigateToQrAddCredentialsPage: () -> Unit,
    navigateToCredentialDetailsPage: (Long) -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    koinScope: Scope,
    vm: CredentialsViewModel = koinViewModel(scope = koinScope),
    bottomBar: @Composable () -> Unit
) {
    val credentialsStatus by vm.storeContainer.collectAsState()
    val credentialTimelinessesStates by vm.credentialTimelinessesStates.collectAsState()

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
                            contentDescription = stringResource(Res.string.content_description_navigate_to_settings),
                        )
                    }
                    Spacer(Modifier.width(15.dp))
                }
            )
        },
        floatingActionButton = {
            when (val it = credentialsStatus) {
                is CredentialStateModel.Success -> {
                    if (it.credentials.isNotEmpty()) {
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
            when (val credentialsStatusDelegate = credentialsStatus) {
                CredentialStateModel.Loading -> Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is CredentialStateModel.Success -> {
                    val credentials = credentialsStatusDelegate.credentials.sortedBy { (id, _) ->
                        if (credentialTimelinessesStates[id]?.isNotBad == true) {
                            0
                        } else {
                            1
                        }
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

                                val isTokenStatusEvaluated = storeEntryIdentifier in credentialTimelinessesStates
                                val credentialFreshnessSummary = credentialTimelinessesStates[storeEntryIdentifier]

                                Column {
                                    CredentialCard(
                                        credential,
                                        // TODO: is this still necessary?
                                        isTokenStatusEvaluated = isTokenStatusEvaluated,
                                        credentialFreshnessSummaryModel = credentialFreshnessSummary,
                                        onDelete = {
                                            vm.removeStoreEntryById(storeEntryIdentifier)
                                        },
                                        onOpenDetails = {
                                            navigateToCredentialDetailsPage(storeEntryIdentifier)
                                        },
                                        imageDecoder = vm::decodeImage,
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
            }
        }
    }
}

private sealed interface CredentialStatusesState {
    val credentialFreshnessSummaries: Map<Long, CredentialFreshnessSummaryUiModel>

    data class Loading(
        override val credentialFreshnessSummaries: Map<Long, CredentialFreshnessSummaryUiModel> = mapOf(),
    ) : CredentialStatusesState

    data class Success(
        override val credentialFreshnessSummaries: Map<Long, CredentialFreshnessSummaryUiModel> = mapOf(),
    ) : CredentialStatusesState
}
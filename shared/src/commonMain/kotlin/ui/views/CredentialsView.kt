package ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_my_data_screen
import org.jetbrains.compose.resources.stringResource
import ui.composables.CustomFloatingActionMenu
import ui.composables.FloatingActionButtonHeightSpacer
import ui.composables.credentials.CredentialCard
import ui.viewmodels.CredentialsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialsView(
    vm: CredentialsViewModel,
    bottomBar: @Composable () -> Unit
) {
    val storeContainerState by vm.storeContainer.collectAsState(null)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.heading_label_my_data_screen),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
            )
        },
        floatingActionButton = {
            storeContainerState?.let { storeContainer ->
                if (storeContainer.credentials.isNotEmpty()) {
                    CustomFloatingActionMenu(addCredential = vm.navigateToAddCredentialsPage, addCredentialQr = vm.navigateToQrAddCredentialsPage)
                }
            }
        },
        bottomBar = { bottomBar() }
    ) { scaffoldPadding ->
        Column(modifier = Modifier.padding(scaffoldPadding).fillMaxSize()) {
            storeContainerState?.let { storeContainer ->
                if (storeContainer.credentials.isEmpty()) {
                    NoDataLoadedView(vm.navigateToAddCredentialsPage, vm.navigateToQrAddCredentialsPage)
                } else {
                    LazyColumn {
                        items(
                            storeContainer.credentials.size,
                            key = {
                                storeContainer.credentials[it].hashCode()
                            }
                        ) { index ->
                            val storeEntry = storeContainer.credentials[index]
                            val storeEntryIdentifier = storeEntry.first
                            val credential = storeEntry.second

                            Column {
                                CredentialCard(
                                    credential,
                                    onDelete = {
                                        vm.removeStoreEntryById(storeEntryIdentifier)
                                    },
                                    onOpenDetails = {
                                        vm.navigateToCredentialDetailsPage(storeEntryIdentifier)
                                    },
                                    imageDecoder = vm.imageDecoder,
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

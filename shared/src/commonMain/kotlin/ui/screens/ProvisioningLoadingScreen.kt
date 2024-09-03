package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationContext
import at.asitplus.wallet.app.common.WalletMain
import composewalletapp.shared.generated.resources.heading_label_load_data_screen
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.snackbar_credential_loaded_successfully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ui.composables.BiometryPrompt
import ui.composables.buttons.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun ProvisioningLoadingScreen(
    link: String,
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
    val authorizationContext = provisioningAuthorizationContext()
    var showBiometry by rememberSaveable {
        mutableStateOf(true)
    }

    var currentLoadingJob by rememberSaveable {
        mutableStateOf<Job?>(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.heading_label_load_data_screen),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = {
                    NavigateUpButton(
                        onClick = {
                            currentLoadingJob?.cancel()
                        }
                    )
                }
            )
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            walletMain.cryptoService.onUnauthenticated = navigateUp
            showBiometry = false
            currentLoadingJob = walletMain.scope.launch {
                try {
                    walletMain.cryptoService.useAuthorizationContext(authorizationContext) {
                        walletMain.provisioningService.handleResponse(link)
                    }.getOrThrow()
                    walletMain.snackbarService.showSnackbar(
                        getString(Res.string.snackbar_credential_loaded_successfully)
                    )
                    navigateUp()
                } catch (e: Throwable) {
                    navigateUp()
                    walletMain.errorService.emit(e)
                }
            }


            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxSize(0.5f),
            )
        }
    }
}


@Composable
expect fun provisioningAuthorizationContext(): CryptoServiceAuthorizationContext
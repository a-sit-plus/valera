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
import at.asitplus.wallet.app.common.WalletMain
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.biometric_authentication_prompt_to_bind_credentials_subtitle
import compose_wallet_app.shared.generated.resources.biometric_authentication_prompt_to_bind_credentials_title
import compose_wallet_app.shared.generated.resources.heading_label_load_data_screen
import compose_wallet_app.shared.generated.resources.snackbar_credential_loaded_successfully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvisioningLoadingView(
    link: String,
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
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
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxSize(0.5f),
            )
            currentLoadingJob = CoroutineScope(Dispatchers.Unconfined).launch {
                try {
                    walletMain.cryptoService.promptText =
                        getString(Res.string.biometric_authentication_prompt_to_bind_credentials_title)
                    walletMain.cryptoService.promptSubtitle =
                        getString(Res.string.biometric_authentication_prompt_to_bind_credentials_subtitle)
                    walletMain.provisioningService.handleResponse(link)
                    walletMain.snackbarService.showSnackbar(
                        getString(Res.string.snackbar_credential_loaded_successfully)
                    )
                    navigateUp()
                } catch (e: Throwable) {
                    navigateUp()
                    walletMain.errorService.emit(e)
                }
            }


        }
    }
}

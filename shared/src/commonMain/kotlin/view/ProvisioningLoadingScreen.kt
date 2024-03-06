package view

import Resources
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ui.composables.BiometryPrompt
import ui.composables.buttons.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvisioningLoadingScreen(
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
                        text = Resources.HEADING_LABEL_LOAD_DATA,
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
            if (showBiometry) {
                BiometryPrompt(
                    title = Resources.BIOMETRIC_AUTHENTICATION_PROMPT_TO_LOAD_DATA_TITLE,
                    subtitle = Resources.BIOMETRIC_AUTHENTICATION_PROMPT_TO_LOAD_DATA_SUBTITLE,
                    onDismiss = {
                        showBiometry = false
                        navigateUp()
                    },
                    onSuccess = {
                        showBiometry = false
                        currentLoadingJob = walletMain.scope.launch {
                            try {
                                walletMain.provisioningService.handleResponse(link)
                                walletMain.snackbarService.showSnackbar(Resources.SNACKBAR_CREDENTIAL_LOADED_SUCCESSFULLY)
                                navigateUp()
                            } catch (e: Throwable) {
                                navigateUp()
                                walletMain.errorService.emit(e)
                            }
                        }
                    }
                )
            }
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxSize(0.5f),
            )
        }
    }
}
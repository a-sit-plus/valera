package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_loading_screen
import at.asitplus.valera.resources.loading_message_app_initialization
import at.asitplus.valera.resources.loading_message_attestation_settings
import at.asitplus.valera.resources.loading_message_checking_device_requirements
import at.asitplus.valera.resources.loading_message_checking_requested_credentials
import at.asitplus.valera.resources.loading_message_credential_offer
import at.asitplus.valera.resources.loading_message_generic
import at.asitplus.valera.resources.loading_message_incoming_request
import at.asitplus.valera.resources.loading_message_issuer_metadata
import at.asitplus.valera.resources.loading_message_issuing_credential
import at.asitplus.valera.resources.loading_message_requesting_camera_permission
import at.asitplus.valera.resources.loading_message_scanning_qr_content
import at.asitplus.valera.resources.loading_message_storing_credential
import at.asitplus.wallet.app.common.LoadingMessageKey
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton

@Composable
fun loadingMessageString(message: LoadingMessageKey): String =
    stringResource(message.stringResource)

private val LoadingMessageKey.stringResource: StringResource
    get() = when (this) {
        LoadingMessageKey.AppInitialization -> Res.string.loading_message_app_initialization
        LoadingMessageKey.IncomingRequest -> Res.string.loading_message_incoming_request
        LoadingMessageKey.CredentialOffer -> Res.string.loading_message_credential_offer
        LoadingMessageKey.IssuerMetadata -> Res.string.loading_message_issuer_metadata
        LoadingMessageKey.IssuingCredential -> Res.string.loading_message_issuing_credential
        LoadingMessageKey.StoringCredential -> Res.string.loading_message_storing_credential
        LoadingMessageKey.CheckingRequestedCredentials -> Res.string.loading_message_checking_requested_credentials
        LoadingMessageKey.CheckingDeviceRequirements -> Res.string.loading_message_checking_device_requirements
        LoadingMessageKey.RequestingCameraPermission -> Res.string.loading_message_requesting_camera_permission
        LoadingMessageKey.ScanningQrContent -> Res.string.loading_message_scanning_qr_content
        LoadingMessageKey.AttestationSettings -> Res.string.loading_message_attestation_settings
        LoadingMessageKey.Generic -> Res.string.loading_message_generic
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingView(
    customLabel: String? = null,
    navigateUp: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ScreenHeading(stringResource(Res.string.heading_label_loading_screen))
                },
                navigationIcon = {
                    navigateUp?.let { NavigateUpButton(onClick = navigateUp) }
                }
            )
        }
    ) { scaffoldPadding ->
        LoadingViewBody(scaffoldPadding, customLabel, action)
    }
}

@Composable
fun LoadingViewBody(
    scaffoldPadding: PaddingValues,
    customLabel: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.padding(scaffoldPadding).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxSize(0.5f)
        )
        customLabel?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        action?.let {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                it()
            }
        }
    }
}

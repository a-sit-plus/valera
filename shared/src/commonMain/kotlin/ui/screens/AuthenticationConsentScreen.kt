package ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationContext
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.data.dif.PresentationDefinition
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParametersFrom
import at.asitplus.wallet.lib.oidc.helpers.AuthorizationResponsePreparationState
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.error_authentication_at_sp_failed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import ui.views.AuthenticationConsentView

@Composable
fun AuthenticationConsentScreen(
    spName: String,
    spLocation: String,
    spImage: ImageBitmap?,
    authenticationRequestParametersFrom: AuthenticationRequestParametersFrom,
    authorizationResponsePreparationState: AuthorizationResponsePreparationState,
    fromQrCodeScanner: Boolean,
    navigateUp: () -> Unit,
    navigateToAuthenticationSuccessPage: () -> Unit,
    walletMain: WalletMain,
) {
    StatefulAuthenticationConsentView(
        spName = spName,
        spLocation = spLocation,
        spImage = spImage,
        authenticationRequest = authenticationRequestParametersFrom,
        fromQrCodeScanner = fromQrCodeScanner,
        navigateUp = navigateUp,
        navigateToAuthenticationSuccessPage = navigateToAuthenticationSuccessPage,
        walletMain = walletMain,
    )
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun StatefulAuthenticationConsentView(
    spName: String,
    spLocation: String,
    spImage: ImageBitmap?,
    authenticationRequest: AuthenticationRequestParametersFrom,
    fromQrCodeScanner: Boolean,
    navigateUp: () -> Unit,
    navigateToAuthenticationSuccessPage: () -> Unit,
    walletMain: WalletMain,
) {
    var showBiometry by rememberSaveable { mutableStateOf(false) }
    val authorizationContext = presentationAuthorizationContext()

    AuthenticationConsentView(
        spName = spName,
        spLocation = spLocation,
        spImage = spImage,
        navigateUp = navigateUp,
        cancelAuthentication = navigateUp,
        consentToDataTransmission = {
            showBiometry = true
        },
        authorizationContext = authorizationContext,
        showBiometry = showBiometry,
        onBiometrySuccess = {
            showBiometry = false
            walletMain.scope.launch {
                try {
                    walletMain.cryptoService.useAuthorizationContext(authorizationContext) {
                        walletMain.presentationService.startSiop(
                            authenticationRequest,
                            fromQrCodeScanner
                        )
                    }.getOrThrow()
                    navigateUp()
                    navigateToAuthenticationSuccessPage()
                } catch (e: Throwable) {
                    walletMain.errorService.emit(e)
                    walletMain.snackbarService.showSnackbar(getString(Res.string.error_authentication_at_sp_failed))
                }
            }
        },
        onBiometryDismissed = { biometryPromptDismissResult ->
            walletMain.snackbarService.showSnackbar(biometryPromptDismissResult.errorString)
            showBiometry = false
        },
    )
}

val PresentationDefinition.claims: List<String>
    get() = this.inputDescriptors
        .mapNotNull { it.constraints }.flatMap { it.fields?.toList() ?: listOf() }
        .flatMap { it.path.toList() }
        .filter { it != "$.type" }
        .filter { it != "$.mdoc.doctype" }
        .filter { it != "$.mdoc.namespace" }
        .map { it.removePrefix("\$.mdoc.") }
        .map { it.removePrefix("\$.") }
        .map { it.removePrefix("\$['org.iso.18013.5.1']['").removeSuffix("']") }
        .map { it.removePrefix("\$['eu.europa.ec.eudiw.pid.1']['").removeSuffix("']") }


@Composable
expect fun presentationAuthorizationContext(): CryptoServiceAuthorizationContext
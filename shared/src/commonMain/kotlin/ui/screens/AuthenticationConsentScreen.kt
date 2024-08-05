package ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationPromptContext
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.data.dif.PresentationDefinition
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParametersFrom
import at.asitplus.wallet.lib.oidc.AuthenticationResponseResult
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.error_authentication_at_sp_failed
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
    authenticationResponseResult: AuthenticationResponseResult,
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
    val authorizationPromptContext = presentationAuthorizationPromptContext()

    AuthenticationConsentView(
        spName = spName,
        spLocation = spLocation,
        spImage = spImage,
        navigateUp = navigateUp,
        cancelAuthentication = navigateUp,
        consentToDataTransmission = {
            walletMain.scope.launch {
                try {
                    walletMain.cryptoService.runWithAuthorizationPrompt(authorizationPromptContext) {
                        walletMain.presentationService.startSiop(
                            authenticationRequest,
                            fromQrCodeScanner
                        )
                    }
                    navigateUp()
                    navigateToAuthenticationSuccessPage()
                } catch (e: Throwable) {
                    walletMain.errorService.emit(e)
                    walletMain.snackbarService.showSnackbar(getString(Res.string.error_authentication_at_sp_failed))
                }
            }
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
expect fun presentationAuthorizationPromptContext(): CryptoServiceAuthorizationPromptContext
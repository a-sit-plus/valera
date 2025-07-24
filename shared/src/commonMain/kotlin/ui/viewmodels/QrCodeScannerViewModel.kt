package ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.WalletMain
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ui.navigation.routes.AddCredentialPreAuthnRoute
import ui.navigation.routes.AuthenticationViewRoute
import ui.navigation.routes.Route
import ui.navigation.routes.SigningQtspSelectionRoute

class QrCodeScannerViewModel(
    val navigateUp: (() -> Unit)?,
    val onSuccess: (Route) -> Unit,
    val onFailure: (Throwable) -> Unit,
    val walletMain: WalletMain,
    val onClickLogo: () -> Unit,
    val onClickSettings: () -> Unit,
    val mode: QrCodeScannerMode
) {
    var isLoading by mutableStateOf(false)

    suspend fun startModeProcess(mode: QrCodeScannerMode, link: String) = when (mode) {
        QrCodeScannerMode.AUTHENTICATION -> prepareAuthentication(link)
        QrCodeScannerMode.SIGNING -> prepareSigning(link)
        QrCodeScannerMode.PROVISIONING -> prepareCredential(link)

    }

    fun onQrScanned(link: String) = walletMain.scope.launch {
        isLoading = true
        Napier.d("onQrScanned: $link")
        listOf(mode).plus(QrCodeScannerMode.entries.filter { it != mode }).forEach {
            catchingUnwrapped {
                startModeProcess(it, link)
            }.onSuccess {
                isLoading = false
                onSuccess(it)
                return@launch
            }
        }
        onFailure(Throwable("Unable to parse: $link"))
    }

    suspend fun prepareCredential(link: String) = catchingUnwrapped {
        AddCredentialPreAuthnRoute(walletMain.provisioningService.decodeCredentialOffer(link))
    }.onFailure {
        Napier.w("Error parsing credential offer", it)
    }.getOrThrow()


    suspend fun prepareAuthentication(link: String) = catchingUnwrapped {
        val buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase =
            BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
                presentationService = walletMain.presentationService,
            )
        val page =
            buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(link).getOrThrow()
        AuthenticationViewRoute(
            authenticationRequest = page.authenticationRequest,
            authorizationResponsePreparationState = page.authorizationResponsePreparationState,
            recipientLocation = page.recipientLocation,
            isCrossDeviceFlow = true
        )
    }.getOrThrow()


    suspend fun prepareSigning(link: String) = catchingUnwrapped {
        SigningQtspSelectionRoute(walletMain.signingService.parseSignatureRequestParameter(link))
    }.getOrThrow()
}

@Serializable
enum class QrCodeScannerMode() {
    SIGNING,
    AUTHENTICATION,
    PROVISIONING
}
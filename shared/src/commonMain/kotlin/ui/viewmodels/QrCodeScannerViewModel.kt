package ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import at.asitplus.catchingUnwrapped
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_cross_device_qr_code_use_system_camera
import at.asitplus.wallet.app.common.WalletMain
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import ui.navigation.routes.AddCredentialPreAuthnRoute
import ui.navigation.routes.AuthenticationViewRoute
import ui.navigation.routes.QrCodeScannerRoute
import ui.navigation.routes.Route
import ui.navigation.routes.SigningQtspSelectionRoute

class QrCodeScannerViewModel(
    savedStateHandle: SavedStateHandle,
    val walletMain: WalletMain,
) : ViewModel() {
    val mode = savedStateHandle.toRoute<QrCodeScannerRoute>().mode

    suspend fun startModeProcess(mode: QrCodeScannerMode, link: String) = when (mode) {
        QrCodeScannerMode.AUTHENTICATION -> prepareAuthentication(link)
        QrCodeScannerMode.SIGNING -> prepareSigning(link)
        QrCodeScannerMode.PROVISIONING -> prepareCredential(link)
    }

    fun onQrScanned(
        link: String,
        onSuccess: (Route) -> Unit,
        onFailure: (Throwable) -> Unit,
        onCrossDeviceQrCode: (forwarded: Boolean) -> Unit,
    ) = walletMain.scope.launch {
        if (link.isFidoCrossDeviceQrCode()) {
            val forwarded = walletMain.platformAdapter.tryOpenCrossDeviceQrCode(link)
            if (!forwarded) {
                walletMain.snackbarService.showSnackbar(
                    getString(Res.string.snackbar_cross_device_qr_code_use_system_camera)
                )
            }
            onCrossDeviceQrCode(forwarded)
            return@launch
        }

        Napier.d("onQrScanned: $link")
        val orderedModes = listOf(mode).plus(QrCodeScannerMode.entries.filter { it != mode })
        val failures = mutableListOf<Throwable>()
        for (currentMode in orderedModes) {
            startModeProcess(currentMode, link).fold(
                onSuccess = { onSuccess(it); return@launch },
                onFailure = { failures += it },
            )
        }
        // No mode could handle the link. Surface the real cause from the selected mode (tried
        // first) instead of a generic message, so e.g. an HTTP error while dereferencing
        // `request_uri` reaches the user. Fall back to the generic message only if no cause exists.
        onFailure(failures.firstOrNull() ?: Throwable("Unable to parse: $link"))
    }

    suspend fun prepareCredential(link: String) = catchingUnwrapped {
        AddCredentialPreAuthnRoute(walletMain.provisioningService.decodeCredentialOffer(link))
    }.onFailure {
        Napier.w("Error parsing credential offer", it)
    }


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
    }


    suspend fun prepareSigning(link: String) = catchingUnwrapped {
        SigningQtspSelectionRoute(walletMain.signingService.parseSignatureRequestParameter(link))
    }
}

@Serializable
enum class QrCodeScannerMode() {
    SIGNING,
    AUTHENTICATION,
    PROVISIONING
}

internal fun String.isFidoCrossDeviceQrCode(): Boolean =
    startsWith(FIDO_CROSS_DEVICE_QR_PREFIX, ignoreCase = true) &&
            getOrNull(FIDO_CROSS_DEVICE_QR_PREFIX.length) in '0'..'9'

private const val FIDO_CROSS_DEVICE_QR_PREFIX = "FIDO:/"

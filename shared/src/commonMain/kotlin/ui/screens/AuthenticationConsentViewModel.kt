package ui.screens

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParametersFrom

class AuthenticationConsentViewModel(
    val spName: String?,
    val spLocation: String,
    val spImage: ImageBitmap?,
    val authenticationRequest: AuthenticationRequestParametersFrom,
    val navigateUp: () -> Unit,
    val navigateToAuthenticationSuccessPage: () -> Unit,
    val walletMain: WalletMain,
)
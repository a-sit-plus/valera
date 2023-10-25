package view

import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.WalletMain

@Composable
expect fun CameraView(onFoundPayload: (text: String) -> Unit, walletMain: WalletMain)


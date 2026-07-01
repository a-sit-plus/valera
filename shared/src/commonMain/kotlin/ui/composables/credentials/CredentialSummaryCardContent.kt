package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import ui.models.ResolvedCredential

@Composable
fun CredentialSummaryCardContent(
    credential: ResolvedCredential,
    decodeToBitmap: (ByteArray) -> Result<ImageBitmap>,
) {
    credential.scheme.let { s ->
        when {
            s.isEuPid -> EuPidCredentialSummaryCardContent(credential, decodeToBitmap)
            s.isMdl -> MobileDrivingLicenceCredentialSummaryCardContent(credential, decodeToBitmap)
            else -> {}
        }
    }
}

package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

@Composable
fun CredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    decodeToBitmap: (ByteArray) -> Result<ImageBitmap>,
) {
    @Suppress("DEPRECATION")
    credential.scheme.let { s ->
        when {
            s.isEuPid -> EuPidCredentialSummaryCardContent(credential, decodeToBitmap)
            s.isMdl -> MobileDrivingLicenceCredentialSummaryCardContent(credential, decodeToBitmap)
            else -> {}
        }
    }
}

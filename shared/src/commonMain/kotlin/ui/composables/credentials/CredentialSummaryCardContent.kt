package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import data.credentials.CredentialAdapter
import data.credentials.EuPidCredentialAdapter
import data.credentials.MobileDrivingLicenceCredentialAdapter
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

@Composable
fun CredentialSummaryCardContent(
    credentialAdapter: CredentialAdapter,
) {
    when (credentialAdapter) {
        is EuPidCredentialAdapter -> EuPidCredentialIdentityDataCardContent(credentialAdapter)
        is MobileDrivingLicenceCredentialAdapter -> MobileDrivingLicenceCredentialIdentityDataCardContent(
            credentialAdapter
        )

        else -> Unit
    }
}

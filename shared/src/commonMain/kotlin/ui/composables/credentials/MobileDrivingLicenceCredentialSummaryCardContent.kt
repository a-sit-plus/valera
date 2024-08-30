package ui.composables.credentials

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.content_description_portrait
import data.credentials.EuPidCredentialAdapter
import data.credentials.IdAustriaCredentialAdapter
import data.credentials.MobileDrivingLicenceCredentialAdapter
import org.jetbrains.compose.resources.stringResource

@Composable
fun MobileDrivingLicenceCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    decodeToBitmap: (ByteArray) -> ImageBitmap,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember {
        MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(credential, decodePortrait = decodeToBitmap)
    }

    MobileDrivingLicenceCredentialIdentityDataCardContent(
        credentialAdapter = credentialAdapter,
    )
}
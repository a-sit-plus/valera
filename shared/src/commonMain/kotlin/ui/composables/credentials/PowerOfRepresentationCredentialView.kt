package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.MobileDrivingLicenceCredentialAdapter

@Composable
fun PowerOfRepresentationCredentialView(
    credential: SubjectCredentialStore.StoreEntry,
    decodeImage: (ByteArray) -> ImageBitmap,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember {
        MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(
            credential,
            decodePortrait = decodeImage,
        )
    }

    Column(modifier = modifier) {
        val spacingModifier = Modifier.padding(bottom = 16.dp)
        PowerOfRepresentationCredentialIdentityDataCard(
            credentialAdapter = credentialAdapter,
            modifier = spacingModifier,
        )
        PowerOfRepresentationCredentialAgeDataCard(
            credentialAdapter = credentialAdapter,
            modifier = spacingModifier,
        )
        PowerOfRepresentationCredentialResidenceDataCard(
            credentialAdapter = credentialAdapter,
            modifier = spacingModifier,
        )
    }
}
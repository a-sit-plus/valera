package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.IdAustriaCredentialAdapter

@Composable
fun IdAustriaCredentialView(
    credential: SubjectCredentialStore.StoreEntry,
    imageDecoder: (ByteArray) -> Result<ImageBitmap>,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember {
        IdAustriaCredentialAdapter.createFromStoreEntry(credential, imageDecoder)
    }

    Column(modifier = modifier) {
        val spacingModifier = Modifier.padding(bottom = 16.dp)
        IdAustriaCredentialIdentityDataCard(
            credentialAdapter = credentialAdapter,
            modifier = spacingModifier,
        )
        IdAustriaCredentialAgeDataCard(
            credentialAdapter = credentialAdapter,
            modifier = spacingModifier,
        )
        IdAustriaCredentialResidenceDataCard(
            credentialAdapter = credentialAdapter,
            modifier = spacingModifier,
        )
    }
}
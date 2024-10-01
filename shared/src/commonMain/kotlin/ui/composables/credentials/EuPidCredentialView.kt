package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.EuPidCredentialAdapter
import data.credentials.IdAustriaCredentialAdapter

@Composable
fun EuPidCredentialView(
    credential: SubjectCredentialStore.StoreEntry,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember {
        EuPidCredentialAdapter.createFromStoreEntry(credential)
    }

    Column(modifier = modifier) {
        val spacingModifier = Modifier.padding(bottom = 16.dp)
        EuPidCredentialIdentityDataCard(credentialAdapter, spacingModifier)
        EuPidCredentialAgeDataCard(credentialAdapter, spacingModifier)
        EuPidCredentialResidenceDataCard(credentialAdapter, spacingModifier)
        EuPidCredentialMetadataCard(credentialAdapter, spacingModifier)
    }
}
package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.AgeVerificationCredentialAdapter

@Composable
fun AgeVerificationCredentialView(
    credential: SubjectCredentialStore.StoreEntry,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember {
        AgeVerificationCredentialAdapter.createFromStoreEntry(credential)
    }
    AgeVerificationCredentialViewFromAdapter(credentialAdapter, modifier)
}

@Composable
fun AgeVerificationCredentialViewFromAdapter(
    credentialAdapter: AgeVerificationCredentialAdapter,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val spacingModifier = Modifier.padding(bottom = 16.dp)
        AgeVerificationCredentialAgeDataCard(
            credentialAdapter = credentialAdapter,
            modifier = spacingModifier,
        )
    }
}

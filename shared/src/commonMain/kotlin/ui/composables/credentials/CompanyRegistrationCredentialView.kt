package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.CompanyRegistrationCredentialAdapter

@Composable
fun CompanyRegistrationCredentialView(
    credential: SubjectCredentialStore.StoreEntry,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember {
        CompanyRegistrationCredentialAdapter.createFromStoreEntry(credential)
    }

    Column(modifier = modifier) {
        val spacingModifier = Modifier.padding(bottom = 16.dp)
        CompanyRegistrationCredentialCompanyDataCard(
            credentialAdapter = credentialAdapter,
            modifier = spacingModifier,
        )
        CompanyRegistrationCredentialMetaDataCard(
            credentialAdapter = credentialAdapter,
            modifier = spacingModifier,
        )
    }
}

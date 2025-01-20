package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.CompanyRegistrationCredentialAdapter

@Composable
fun CompanyRegistrationCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
) {
    val credentialAdapter = remember {
        CompanyRegistrationCredentialAdapter.createFromStoreEntry(credential)
    }
    CompanyRegistrationCredentialCompanyDataCardContent(credentialAdapter)
}
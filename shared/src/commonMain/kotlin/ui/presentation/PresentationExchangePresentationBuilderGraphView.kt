package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult
import ui.models.toCredentialFreshnessSummaryModel

@ExperimentalMaterial3Api
@Composable
fun PresentationExchangePresentationBuilderGraphView(
    matchingResult: PresentationExchangeMatchingResult<SubjectCredentialStore.StoreEntry>,
) {

    Text("PresentationExchangePresentationBuilderGraphView")
}
package ui.composables.credentials

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import data.credentials.CredentialAdapter
import ui.models.CredentialFreshnessSummaryUiModel
import ui.models.ResolvedCredential

@Composable
fun CredentialCard(
    credential: ResolvedCredential,
    isTokenStatusEvaluated: Boolean,
    credentialFreshnessSummaryModel: CredentialFreshnessSummaryUiModel?,
    imageDecoder: (ByteArray) -> Result<ImageBitmap>,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDetails: (() -> Unit)?,
    showActionMenu: Boolean = true,
    summaryAdapter: CredentialAdapter? = null,
    usePreparedSummary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    CredentialCardLayout(
        colors = if (credentialFreshnessSummaryModel?.isNotBad == false) {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        } else {
            CardDefaults.elevatedCardColors()
        },
        modifier = modifier,
    ) {
        CredentialCardHeader(
            credential = credential,
            showLoadingSpinner = !isTokenStatusEvaluated,
            showActionMenu = showActionMenu,
            onDelete = onDelete,
            onRefresh = onRefresh,
            credentialFreshnessSummaryModel = credentialFreshnessSummaryModel
        )
        if (usePreparedSummary) {
            summaryAdapter?.let { CredentialSummaryCardContent(it) }
        } else {
            CredentialSummaryCardContent(
                credential = credential,
                decodeToBitmap = imageDecoder,
            )
        }
        CredentialCardFooter(
            credentialFreshnessSummaryModel = credentialFreshnessSummaryModel,
            onOpenDetails,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

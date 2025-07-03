package ui.composables.credentials

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import ui.models.CredentialFreshnessSummaryUiModel

@Composable
fun CredentialCard(
    credential: SubjectCredentialStore.StoreEntry,
    isTokenStatusEvaluated: Boolean,
    credentialFreshnessSummaryModel: CredentialFreshnessSummaryUiModel?,
    imageDecoder: (ByteArray) -> Result<ImageBitmap>,
    onDelete: () -> Unit,
    onOpenDetails: (() -> Unit)?,
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
            onDelete = onDelete,
        )
        CredentialSummaryCardContent(
            credential = credential,
            decodeToBitmap = imageDecoder,
        )
        CredentialCardFooter(
            credentialFreshnessSummaryModel = credentialFreshnessSummaryModel,
            onOpenDetails,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

package ui.composables.credentials

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus

@Composable
fun CredentialCard(
    credential: SubjectCredentialStore.StoreEntry,
    isTokenStatusEvaluated: Boolean,
    tokenStatus: TokenStatus?,
    imageDecoder: (ByteArray) -> ImageBitmap?,
    onDelete: () -> Unit,
    onOpenDetails: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    CredentialCardLayout(
        colors = when(tokenStatus) {
            TokenStatus.Invalid -> CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
            else -> CardDefaults.elevatedCardColors()
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
            isTokenStatusEvaluated = isTokenStatusEvaluated,
            tokenStatus = tokenStatus,
            onOpenDetails,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

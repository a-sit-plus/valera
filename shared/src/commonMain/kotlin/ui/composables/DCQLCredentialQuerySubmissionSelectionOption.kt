package ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import at.asitplus.openid.dcql.DCQLCredentialSubmissionOption
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import ui.composables.credentials.CredentialSelectionCardHeader
import ui.composables.credentials.CredentialSelectionCardLayout
import ui.composables.credentials.CredentialSummaryCardContent


@Composable
fun DCQLCredentialQuerySubmissionSelectionOption(
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    option: DCQLCredentialSubmissionOption<SubjectCredentialStore.StoreEntry>,
    decodeToBitmap: (ByteArray) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    val credential = option.credential
    val matchingResult = option.matchingResult

    CredentialSelectionCardLayout(
        onClick = onToggleSelection,
        isSelected = isSelected,
        modifier = modifier,
    ) {
        CredentialSelectionCardHeader(
            credential = credential,
            modifier = Modifier.fillMaxWidth()
        )
        CredentialSummaryCardContent(
            credential = credential,
            decodeToBitmap = decodeToBitmap,
        )
    }

    val density = LocalDensity.current
    AnimatedVisibility(
        visible = isSelected,
        enter = slideInVertically {
            with(density) { -20.dp.roundToPx() }
        } + expandVertically(
            expandFrom = Alignment.Top
        ) + fadeIn(
            initialAlpha = 0.3f
        ),
        exit = slideOutVertically {
            with(density) { 20.dp.roundToPx() }
        } + shrinkVertically(
            shrinkTowards = Alignment.Bottom
        ) + fadeOut(
            targetAlpha = 0f
        )
    ) {
        Text("Is Selected")
    }
}
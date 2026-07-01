package ui.composables.credentials

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import at.asitplus.catchingUnwrapped
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import ui.models.CredentialFreshnessSummaryUiModel
import ui.models.CredentialFreshnessValidationStateUiModel
import ui.models.ResolvedCredential
import ui.models.toFallbackResolvedCredential
import ui.models.toResolvedCredential

@Composable
fun CredentialSelectionCard(
    credential: Map.Entry<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList>>,
    checkCredentialFreshness: suspend () -> CredentialFreshnessSummaryUiModel,
    imageDecoder: (ByteArray) -> Result<ImageBitmap>,
    attributeSelection: SnapshotStateMap<String, Boolean>,
    credentialSelection: MutableState<SubjectCredentialStore.StoreEntry>
) {
    val selected = remember { mutableStateOf(false) }
    selected.value = credentialSelection.value == credential.key
    val resolvedCredential by produceState<ResolvedCredential?>(null, credential.key) {
        value = catchingUnwrapped { credential.key.toResolvedCredential() }
            .getOrElse { credential.key.toFallbackResolvedCredential() }
    }

    val credentialFreshnessValidationState by produceState(
        CredentialFreshnessValidationStateUiModel.Loading as CredentialFreshnessValidationStateUiModel,
        credential.key,
    ) {
        value = CredentialFreshnessValidationStateUiModel.Loading
        value = CredentialFreshnessValidationStateUiModel.Done(checkCredentialFreshness())
    }

    CredentialSelectionCardLayout(
        onClick = {
            attributeSelection.clear()
            credentialSelection.value = credential.key
        },
        modifier = Modifier,
        isSelected = selected.value,
        isError = when (val it = credentialFreshnessValidationState) {
            is CredentialFreshnessValidationStateUiModel.Done -> !it.credentialFreshnessSummary.isNotBad
            CredentialFreshnessValidationStateUiModel.Loading -> false
        },
    ) {
        val displayCredential = resolvedCredential ?: return@CredentialSelectionCardLayout
        CredentialSelectionCardHeader(
            credentialFreshnessValidationState = credentialFreshnessValidationState,
            credential = displayCredential,
            modifier = Modifier.fillMaxWidth(),
            allowMultiSelection = false,
            matchingException = null,
        )
        CredentialSummaryCardContent(
            credential = displayCredential,
            decodeToBitmap = imageDecoder,
        )

        val density = LocalDensity.current
        AnimatedVisibility(
            visible = selected.value,
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
            AttributeSelectionGroup(credential, format = displayCredential.scheme, selection = attributeSelection)
        }
    }
}

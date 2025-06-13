package ui.composables.credentials

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.models.CredentialFreshnessValidationStateUiModel

@Composable
fun CredentialSelectionCardLayout(
    credentialFreshnessValidationState: CredentialFreshnessValidationStateUiModel,
    onClick: () -> Unit,
    modifier: Modifier,
    isSelected: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerColor = mutableStateOf(Color.Unspecified)
    val borderStroke: MutableState<BorderStroke?> = mutableStateOf(null)

    if (isSelected) {
        containerColor.value = when (credentialFreshnessValidationState) {
            CredentialFreshnessValidationStateUiModel.Loading -> MaterialTheme.colorScheme.primaryContainer
            is CredentialFreshnessValidationStateUiModel.Done -> if (credentialFreshnessValidationState.credentialFreshnessSummary.isNotBad) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        }

        val width = 2.dp
        borderStroke.value = when (credentialFreshnessValidationState) {
            CredentialFreshnessValidationStateUiModel.Loading -> BorderStroke(
                width = width,
                color = MaterialTheme.colorScheme.inversePrimary,
            )

            is CredentialFreshnessValidationStateUiModel.Done -> if (credentialFreshnessValidationState.credentialFreshnessSummary.isNotBad) {
                BorderStroke(width = width, color = MaterialTheme.colorScheme.inversePrimary)
            } else {
                BorderStroke(width = width, color = MaterialTheme.colorScheme.error)
            }
        }

    } else {
        containerColor.value = when (credentialFreshnessValidationState) {
            CredentialFreshnessValidationStateUiModel.Loading -> Color.Unspecified
            is CredentialFreshnessValidationStateUiModel.Done -> if (credentialFreshnessValidationState.credentialFreshnessSummary.isNotBad) {
                Color.Unspecified
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        }
        borderStroke.value = null
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor.value),
        border = borderStroke.value
    ) {
        Column(
            modifier = modifier.padding(8.dp).fillMaxWidth(),
        ) {
            content()
        }
    }
}

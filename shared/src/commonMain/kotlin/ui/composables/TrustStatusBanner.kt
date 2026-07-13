package ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.trust_status_evaluating
import at.asitplus.valera.resources.trust_status_trusted
import at.asitplus.valera.resources.trust_status_unknown
import at.asitplus.valera.resources.trust_status_untrusted
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TrustStatusBanner(trustState: TrustState, modifier: Modifier = Modifier) {
    val (backgroundColor, contentColor, icon, text) = when (trustState) {
        TrustState.TRUSTED -> listOf(
            colorScheme.primaryContainer, colorScheme.onPrimaryContainer,
            Icons.Filled.CheckCircle, Res.string.trust_status_trusted
        )
        TrustState.UNTRUSTED -> listOf(
            colorScheme.errorContainer, colorScheme.onErrorContainer,
            Icons.Filled.Warning, Res.string.trust_status_untrusted
        )
        TrustState.UNKNOWN -> listOf(
            colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer,
            Icons.Filled.Warning, Res.string.trust_status_unknown
        )
        TrustState.EVALUATING -> listOf(
            colorScheme.surfaceVariant, colorScheme.onSurfaceVariant,
            Icons.Filled.Warning, Res.string.trust_status_evaluating
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor as Color)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
            contentDescription = null,
            tint = contentColor as Color
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(text as StringResource),
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

enum class TrustState {
    TRUSTED, UNTRUSTED, UNKNOWN, EVALUATING
}
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TrustStatusBanner(trustState: TrustState, modifier: Modifier = Modifier) {
    val (backgroundColor, contentColor, icon, text) = when (trustState) {
        TrustState.TRUSTED -> listOf(
            Color(0xFFE8F5E9), Color(0xFF2E7D32),
            Icons.Filled.CheckCircle, "Trusted Issuer"
        )
        TrustState.UNTRUSTED -> listOf(
            Color(0xFFFFEBEE), Color(0xFFC62828),
            Icons.Filled.Warning, "Untrusted Issuer"
        )
        TrustState.UNKNOWN -> listOf(
            Color(0xFFFFF8E1), Color(0xFFF57F17),
            Icons.Filled.Warning, "Trust Status Unknown"
        )
        TrustState.EVALUATING -> listOf(
            MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Filled.Warning, "Evaluating Trust..."
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
            text = text as String,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

enum class TrustState {
    TRUSTED, UNTRUSTED, UNKNOWN, EVALUATING
}
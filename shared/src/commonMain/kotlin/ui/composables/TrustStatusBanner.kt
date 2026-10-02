package ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.trust_status_evaluating
import at.asitplus.valera.resources.trust_status_evaluating_verifier
import at.asitplus.valera.resources.trust_status_trusted
import at.asitplus.valera.resources.trust_status_trusted_verifier
import at.asitplus.valera.resources.trust_status_unknown
import at.asitplus.valera.resources.trust_status_unknown_verifier
import at.asitplus.valera.resources.trust_status_untrusted
import at.asitplus.valera.resources.trust_status_untrusted_verifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.theme.LocalExtendedColors

@Composable
fun TrustStatusBanner(trustState: TrustState, modifier: Modifier = Modifier) {
    val text = stringResource(
        when (trustState) {
            TrustState.TRUSTED -> Res.string.trust_status_trusted
            TrustState.UNTRUSTED -> Res.string.trust_status_untrusted
            TrustState.UNKNOWN -> Res.string.trust_status_unknown
            TrustState.EVALUATING -> Res.string.trust_status_evaluating
        }
    )
    TrustStatusBanner(trustState = trustState, text = text, modifier = modifier)
}

@Composable
fun TrustStatusBanner(
    trustState: TrustState,
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val (backgroundColor, contentColor) = trustState.statusColors()
    StatusBanner(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        icon = trustState.statusIcon,
        text = text,
        modifier = modifier,
        onClick = onClick,
        onClickLabel = onClickLabel,
        trailingContent = trailingContent,
    )
}

/** Container and content color of [this] trust state, also used for other statuses shown next to it. */
@Composable
internal fun TrustState.statusColors(): Pair<Color, Color> {
    // Trust decisions are warnings, never errors: red is reserved for faults such as invalid signatures
    val extendedColors = LocalExtendedColors.current
    return when (this) {
        TrustState.TRUSTED -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        TrustState.UNTRUSTED -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        TrustState.UNKNOWN -> extendedColors.cautionContainer to extendedColors.onCautionContainer
        TrustState.EVALUATING -> colorScheme.surfaceVariant to colorScheme.onSurfaceVariant
    }
}

internal val TrustState.statusIcon: ImageVector
    get() = when (this) {
        TrustState.TRUSTED -> Icons.Filled.CheckCircle
        TrustState.UNTRUSTED, TrustState.UNKNOWN, TrustState.EVALUATING -> Icons.Filled.Warning
    }

/** Layout shared by status banners, e.g. of trust or of signature validity. */
@Composable
internal fun StatusBanner(
    backgroundColor: Color,
    contentColor: Color,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClickLabel = onClickLabel,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        trailingContent?.let {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                it()
            }
        }
    }
}

enum class TrustState {
    TRUSTED, UNTRUSTED, UNKNOWN, EVALUATING
}

val TrustState.displayVerifierText: StringResource
    get() = when (this) {
        TrustState.TRUSTED -> Res.string.trust_status_trusted_verifier
        TrustState.UNTRUSTED -> Res.string.trust_status_untrusted_verifier
        TrustState.UNKNOWN -> Res.string.trust_status_unknown_verifier
        TrustState.EVALUATING -> Res.string.trust_status_evaluating_verifier
    }

package ui.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.zk_mode_badge_plain
import at.asitplus.valera.resources.zk_mode_badge_zk_required
import at.asitplus.valera.resources.zk_mode_badge_zk_with_plain_fallback
import at.asitplus.wallet.app.common.ZkMode
import org.jetbrains.compose.resources.stringResource
import ui.theme.LocalExtendedColors

/** Tells the user whether the verifier asks for a plain mdoc or a zero-knowledge proof. */
@Composable
internal fun ZkModeBadge(zkMode: ZkMode, modifier: Modifier = Modifier) {
    if (zkMode == ZkMode.UNKNOWN) return
    val text = stringResource(
        when (zkMode) {
            ZkMode.UNKNOWN, ZkMode.PLAIN -> Res.string.zk_mode_badge_plain
            ZkMode.ZK_WITH_PLAIN_FALLBACK -> Res.string.zk_mode_badge_zk_with_plain_fallback
            ZkMode.ZK_REQUIRED -> Res.string.zk_mode_badge_zk_required
        }
    )
    Surface(
        shape = MaterialTheme.shapes.small,
        color = when (zkMode) {
            ZkMode.PLAIN -> MaterialTheme.colorScheme.tertiaryContainer
            ZkMode.ZK_REQUIRED -> LocalExtendedColors.current.successContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = when (zkMode) {
            ZkMode.PLAIN -> MaterialTheme.colorScheme.onTertiaryContainer
            ZkMode.ZK_REQUIRED -> LocalExtendedColors.current.onSuccessContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** Badge above a card: the larger gap above than below groups it with its own card. */
@Composable
internal fun ZkModeBadgeAboveCard(zkMode: ZkMode) = ZkModeBadge(zkMode, Modifier.padding(top = 8.dp, bottom = 4.dp))

package ui.views

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

@Composable
fun RefreshConfirmationDialog(
    entry: SubjectCredentialStore.StoreEntry,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "A new version of '${entry.scheme.uiLabel()}' is available. Would you like to update it now?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Update Now",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Later",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    )
}
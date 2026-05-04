package ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.refresh_dialog_confirm
import at.asitplus.valera.resources.refresh_dialog_dismiss
import at.asitplus.valera.resources.refresh_dialog_message_multiple
import at.asitplus.valera.resources.refresh_dialog_message_single
import at.asitplus.valera.resources.refresh_dialog_never_show_again
import at.asitplus.valera.resources.refresh_dialog_title_multiple
import at.asitplus.valera.resources.refresh_dialog_title_single
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.displayTitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun RefreshConfirmationDialog(
    entry: SubjectCredentialStore.StoreEntry?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onNeverShowAgain: (() -> Unit)? = null,
) {
    val credentialTitle = entry?.displayTitle(entry.scheme.uiLabel())

    val dialogTitle = if (entry != null) {
        stringResource(Res.string.refresh_dialog_title_single)
    } else {
        stringResource(Res.string.refresh_dialog_title_multiple)
    }

    val dialogMessage = if (entry != null) {
        stringResource(Res.string.refresh_dialog_message_single, credentialTitle ?: entry.scheme.uiLabel())
    } else {
        stringResource(Res.string.refresh_dialog_message_multiple)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = dialogMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(Res.string.refresh_dialog_confirm),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            if (onNeverShowAgain != null) {
                Column {
                    TextButton(onClick = onNeverShowAgain) {
                        Text(
                            text = stringResource(Res.string.refresh_dialog_never_show_again),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(Res.string.refresh_dialog_dismiss),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                Row {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(Res.string.refresh_dialog_dismiss),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    )
}

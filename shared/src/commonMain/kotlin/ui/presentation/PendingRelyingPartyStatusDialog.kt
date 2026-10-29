package ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_send_anyway
import at.asitplus.valera.resources.button_label_wait
import at.asitplus.valera.resources.heading_label_relying_party_status_pending
import at.asitplus.valera.resources.info_text_relying_party_status_pending
import org.jetbrains.compose.resources.stringResource

/**
 * Asks before sending a presentation while the status of the registration certificate of the relying party is still
 * being retrieved: [onSendAnyway] sends it without the status, [onWait] returns to the consent screen, which shows the
 * status once it arrives.
 */
@Composable
fun PendingRelyingPartyStatusDialog(
    onSendAnyway: () -> Unit,
    onWait: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(stringResource(Res.string.heading_label_relying_party_status_pending))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(Res.string.info_text_relying_party_status_pending))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        onDismissRequest = onWait,
        confirmButton = {
            Button(
                onClick = onSendAnyway,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(Res.string.button_label_send_anyway))
            }
        },
        dismissButton = {
            TextButton(onClick = onWait) {
                Text(stringResource(Res.string.button_label_wait))
            }
        },
    )
}

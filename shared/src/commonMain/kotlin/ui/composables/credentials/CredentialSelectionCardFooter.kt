package ui.composables.credentials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import at.asitplus.wallet.lib.agent.SubjectCredentialStore


@Composable
fun ColumnScope.CredentialSelectionCardFooter(
    credential: SubjectCredentialStore.StoreEntry,
    selectedCredential: MutableState<SubjectCredentialStore.StoreEntry>,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = modifier,
    ) {
        RadioButton(selected = (credential == selectedCredential.value),
            onClick = {selectedCredential.value = credential})
    }
}

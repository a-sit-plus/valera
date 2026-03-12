package ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.composables.buttons.BackButton
import ui.composables.buttons.CancelButton
import ui.composables.buttons.ContinueButton

@Composable
fun CommonBottomButtonsAbortContinue(
    text: String?,
    onAbort: () -> Unit,
    onContinue: (() -> Unit)?,
    useBackButton: Boolean = false,
) {
    Surface(
        color = NavigationBarDefaults.containerColor,
    ) {
        Column(
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.Companion.fillMaxWidth().padding(top = 16.dp),
        ) {
            text?.let {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Companion.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    16.dp,
                    alignment = Alignment.CenterHorizontally
                ),
            ) {
                if(useBackButton) {
                    BackButton(onAbort)
                } else {
                    CancelButton(onAbort)
                }
                ContinueButton(onContinue)
            }
        }
    }
}
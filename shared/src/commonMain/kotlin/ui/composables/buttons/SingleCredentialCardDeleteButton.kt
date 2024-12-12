package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.content_description_delete_credential
import org.jetbrains.compose.resources.stringResource


@Composable
fun SingleCredentialCardDeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(Res.string.content_description_delete_credential) // TODO: content description
        )
    }
}
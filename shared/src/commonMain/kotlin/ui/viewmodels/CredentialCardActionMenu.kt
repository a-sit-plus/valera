package ui.viewmodels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.content_description_delete_credential
import compose_wallet_app.shared.generated.resources.button_label_delete_credential
import org.jetbrains.compose.resources.stringResource

@Composable
fun CredentialCardActionMenu(
    onDelete: () -> Unit,
) {
    var isMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    CredentialCardActionMenu(
        isMenuExpanded = isMenuExpanded,
        onChangeIsMenuExpanded = { isMenuExpanded = it },
        onDelete = onDelete,
    )
}

@Composable
fun CredentialCardActionMenu(
    isMenuExpanded: Boolean,
    onChangeIsMenuExpanded: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        IconButton(
            onClick = { onChangeIsMenuExpanded(!isMenuExpanded) },
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null
            )
        }
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = {
                onChangeIsMenuExpanded(false)
            },
        ) {
            DropdownMenuItem(
                text = {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.content_description_delete_credential)
                        )
                        Text(stringResource(Res.string.button_label_delete_credential))
                    }
                },
                onClick = onDelete
            )
        }
    }
}
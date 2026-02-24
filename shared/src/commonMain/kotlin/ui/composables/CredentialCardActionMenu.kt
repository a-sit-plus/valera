package ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_delete_credential
import at.asitplus.valera.resources.button_refresh
import at.asitplus.valera.resources.content_description_delete_credential
import org.jetbrains.compose.resources.stringResource
import ui.models.CredentialFreshnessSummaryUiModel

@Composable
fun CredentialCardActionMenu(
    showLoadingSpinner: Boolean = false,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    credentialFreshnessSummaryModel: CredentialFreshnessSummaryUiModel? = null
) {
    var isMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    CredentialCardActionMenu(
        showLoadingSpinner = showLoadingSpinner,
        isMenuExpanded = isMenuExpanded,
        onChangeIsMenuExpanded = { isMenuExpanded = it },
        onDelete = onDelete,
        onRefresh = onRefresh,
        credentialFreshnessSummaryModel = credentialFreshnessSummaryModel
    )
}

@Composable
fun CredentialCardActionMenu(
    showLoadingSpinner: Boolean,
    isMenuExpanded: Boolean,
    onChangeIsMenuExpanded: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    credentialFreshnessSummaryModel: CredentialFreshnessSummaryUiModel?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        if(showLoadingSpinner) {
            CircularProgressIndicator()
        }
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
            if (credentialFreshnessSummaryModel?.timelinessIndicator?.isExpired == true) {
                DropdownMenuItem(
                    text = {
                        Row {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(Res.string.button_refresh)
                            )
                            Text(stringResource(Res.string.button_refresh))
                        }
                    },
                    onClick = onRefresh
                )
            }
        }
    }
}
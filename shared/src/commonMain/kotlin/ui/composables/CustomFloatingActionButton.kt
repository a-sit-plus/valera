package ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.content_description_add_credential
import org.jetbrains.compose.resources.stringResource

// Modified from https://developer.android.com/develop/ui/compose/animation/composables-modifiers

@Composable
fun CustomFloatingActionButton(showMenu: MutableState<Boolean>, addCredentialQr: () -> Unit, addCredential: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val density = LocalDensity.current
        AnimatedVisibility(
            visible = showMenu.value,
            enter = slideInVertically {
                with(density) { -40.dp.roundToPx() }
            } + expandVertically(
                expandFrom = Alignment.Top
            ) + fadeIn(
                initialAlpha = 0.3f
            ),
            exit = slideOutVertically{
                with(density) { 40.dp.roundToPx() }
            } + shrinkVertically(shrinkTowards = Alignment.Bottom
            ) + fadeOut(
                targetAlpha = 0f
            )
        ) {
            Column {
                SmallFloatingActionButton(onClick = addCredentialQr) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = stringResource(Res.string.content_description_add_credential),
                    )
                }
                SmallFloatingActionButton(onClick = addCredential) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(Res.string.content_description_add_credential),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(20.dp))

        FloatingActionButton(onClick = { showMenu.value = !showMenu.value }) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.content_description_add_credential),
                modifier = Modifier.rotate( if (showMenu.value) 45f else 0f)
            )
        }
    }
}
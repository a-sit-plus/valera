package ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
fun AdvancedFloatingActionButton(addCredentialQr: () -> Unit, addCredential: () -> Unit) {
    val expanded = rememberSaveable{mutableStateOf(false)}

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val density = LocalDensity.current
        AnimatedVisibility(
            visible = expanded.value,
            enter = slideInVertically {
                with(density) { -20.dp.roundToPx() }
            } + expandVertically(
                expandFrom = Alignment.Top
            ) + fadeIn(
                initialAlpha = 0.3f
            ),
            exit = slideOutVertically{
                with(density) { 20.dp.roundToPx() }
            } + shrinkVertically(shrinkTowards = Alignment.Bottom
            ) + fadeOut(
                targetAlpha = 0f
            )
        ) {
            Column (modifier = Modifier.padding(bottom = 20.dp)) {
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
        FloatingActionButton(onClick = { expanded.value = !expanded.value }) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.content_description_add_credential),
                modifier = Modifier.rotate( if (expanded.value) 45f else 0f)
            )
        }
    }
}
package ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_provision_credential_browser
import at.asitplus.valera.resources.button_label_provision_credential_qr
import org.jetbrains.compose.resources.stringResource

// Modified from https://developer.android.com/develop/ui/compose/animation/composables-modifiers

@Composable
fun CustomFloatingActionMenu(addCredentialQr: () -> Unit, addCredential: () -> Unit) {
    val expanded = remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Column(horizontalAlignment = Alignment.End) {
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
                    exit = slideOutVertically {
                        with(density) { 20.dp.roundToPx() }
                    } + shrinkVertically(
                        shrinkTowards = Alignment.Bottom
                    ) + fadeOut(
                        targetAlpha = 0f
                    )
                ) {
                    Column(modifier = Modifier.padding(end = 5.dp)) {
                        SecondaryFloatingActionButton(
                            onClick = { addCredentialQr() },
                            label = stringResource(Res.string.button_label_provision_credential_qr),
                            icon = Icons.Default.QrCode,
                        )
                        Spacer(modifier = Modifier.size(5.dp))
                        SecondaryFloatingActionButton(
                            onClick = { addCredential() },
                            label = stringResource(Res.string.button_label_provision_credential_browser),
                            icon = Icons.Default.Person,
                        )
                    }
                }
                Spacer(modifier = Modifier.size(10.dp))
                MainFloatingActionButton(expanded = expanded)
            }
        }
    }
}

@Composable
fun MainFloatingActionButton(expanded: MutableState<Boolean>) {
    Button(
        onClick = { expanded.value = !expanded.value },
        modifier = Modifier.size(50.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.rotate(if (expanded.value) 45f else 0f)
        )
    }
}

@Composable
fun SecondaryFloatingActionButton(
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
    ) {
        Row(modifier = Modifier.clickable(onClick = onClick)) {
            Column(
                modifier = Modifier.shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(15.dp)
                )
            ) {
                Column(modifier = Modifier.clip(RoundedCornerShape(15.dp))) {
                    TextIconButton(
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                            )
                        },
                        text = {
                            Text(label)
                        },
                        onClick = onClick,
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}
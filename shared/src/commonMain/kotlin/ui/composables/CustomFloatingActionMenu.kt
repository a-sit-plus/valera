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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ui.composables.buttons.LoadDataIdaButton
import ui.composables.buttons.LoadDataQrButton

// Modified from https://developer.android.com/develop/ui/compose/animation/composables-modifiers

@Composable
fun CustomFloatingActionMenu(addCredentialQr: () -> Unit, addCredential: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val onChangeExpanded: (Boolean) -> Unit = { bool ->
        expanded = bool
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Column(horizontalAlignment = Alignment.End) {
                val density = LocalDensity.current
                AnimatedVisibility(
                    visible = expanded,
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
                            onClick = { addCredential() },
                            content = { LoadDataIdaButton(onClick = { addCredential() }) },
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        SecondaryFloatingActionButton(
                            onClick = { addCredential() },
                            content = { LoadDataQrButton(onClick = { addCredentialQr() }) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                MainFloatingActionButton(expanded = expanded, onChangeExpanded = onChangeExpanded)
            }
        }
    }
}

@Composable
fun MainFloatingActionButton(expanded: Boolean, onChangeExpanded: (Boolean) -> Unit) {
    val colors = when (expanded) {
        true -> ButtonDefaults.outlinedButtonColors()
        false -> ButtonDefaults.buttonColors()
    }
    val border = when (expanded) {
        true -> ButtonDefaults.outlinedButtonBorder()
        false -> null
    }
    Button(
        onClick = { onChangeExpanded(!expanded) },
        modifier = Modifier.size(50.dp),
        colors = colors,
        border = border,
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.rotate(if (expanded) 45f else 0f)
        )
    }
}

@Composable
fun SecondaryFloatingActionButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.clickable(onClick = onClick)) {
            Column(
                modifier = Modifier.shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(15.dp)
                )
            ) {
                content()
            }
        }
    }
}
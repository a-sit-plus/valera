package ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration

@Composable
fun DelayedComposable(
    delayDuration: Duration,
    content: @Composable () -> Unit,
) {
    var isDelayOver by rememberSaveable {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        delay(delayDuration)
        isDelayOver = true
    }
    if(isDelayOver) {
        content()
    }
}
package ui.state

import androidx.compose.ui.state.ToggleableState

fun ToggleableState.fromSelection(selection: List<Boolean>): ToggleableState {
    return selection.count { it }.let {
        if (it == selection.size) ToggleableState.On
        else if (it == 0) ToggleableState.Off
        else ToggleableState.Indeterminate
    }
}
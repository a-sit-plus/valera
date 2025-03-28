package ui.state.savers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

private fun <T : Any?> snapshotStateListSaver() = listSaver<SnapshotStateList<T>, T>(
    save = { stateList -> stateList.toList() },
    restore = { it.toMutableStateList() },
)

@Composable
fun <T: Any?> rememberMutableStateListOf(vararg inputs: Any?, init: () -> SnapshotStateList<T>): SnapshotStateList<T> {
    return rememberSaveable(*inputs, saver = snapshotStateListSaver(), init = init)
}
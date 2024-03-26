package ui.savers

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope

private class MutableStateSaver<Original, Saveable : Any>(
    private val saver: ReusableSaver<Original, Saveable>
) : Saver<MutableState<Original>, Saveable> {
    override fun restore(value: Saveable): MutableState<Original>? {
        return saver.restore(value)?.let { mutableStateOf(it) }
    }

    override fun SaverScope.save(value: MutableState<Original>): Saveable? {
        return value.value.let { saver.prepareSaveable(it) }
    }
}

fun <Original, Saveable : Any> ReusableSaver<Original, Saveable>.asMutableStateSaver(): Saver<MutableState<Original>, Saveable> {
    return MutableStateSaver(this)
}
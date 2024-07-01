package ui.state.savers

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope

abstract class ReusableSaver<Original, Saveable : Any> : Saver<Original, Saveable> {
    abstract fun prepareSaveable(value: Original): Saveable?

    override fun SaverScope.save(value: Original): Saveable? {
        return prepareSaveable(value)
    }
}
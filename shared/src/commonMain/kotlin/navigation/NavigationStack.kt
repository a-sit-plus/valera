package navigation

import androidx.compose.runtime.mutableStateListOf

// Copied from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer
class NavigationStack<T>(vararg initial: T) {
    val stack = mutableStateListOf(*initial)
    fun push(t: T) {
        stack.add(t)
    }

    fun back() {
        if (stack.size > 1) {
            // Always keep one element on the view stack
            stack.removeLast()
        }
    }

    fun reset() {
        stack.removeRange(1, stack.size)
    }

    fun lastWithIndex() = stack.withIndex().last()
}
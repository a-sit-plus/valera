package ui.presentation

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import io.github.aakira.napier.Napier

class DCQLPresentationBuilderGraphViewNavigationManager(
    initialStack: List<DCQLPresentationBuilderGraphViewNavigationAction>
) {
    private val navigationStack = mutableStateListOf<DCQLPresentationBuilderGraphViewNavigationAction>().apply {
        addAll(initialStack)
    }

    /**
     * pointing to the *next* action index
     */
    val selectionStack: List<DCQLPresentationBuilderGraphViewModelSelection>
        get() = navigationStack.fold(listOf<DCQLPresentationBuilderGraphViewModelSelection>()) { privateSelectionStack, it ->
            when (it) {
                DCQLPresentationBuilderGraphViewNavigationAction.PopSelection -> privateSelectionStack.subList(
                    0,
                    privateSelectionStack.lastIndex
                )

                DCQLPresentationBuilderGraphViewNavigationAction.PopSelectionsUntilConfirmationInclusive -> {
                    val latestContinueIndex = privateSelectionStack.lastIndexOf(
                        DCQLPresentationBuilderGraphViewModelSelection.ContinueWithSelection
                    )
                    if(latestContinueIndex == -1) {
                        privateSelectionStack
                    } else {
                        privateSelectionStack.subList(
                            0, privateSelectionStack.lastIndexOf(
                                DCQLPresentationBuilderGraphViewModelSelection.ContinueWithSelection
                            ).coerceAtLeast(0)
                        )
                    }
                }

                is DCQLPresentationBuilderGraphViewNavigationAction.PushSelection -> privateSelectionStack + it.action
            }
        }

    fun pushSelection(selection: DCQLPresentationBuilderGraphViewModelSelection) {
        // back and then continue are inverse, we want to be able to go back and verify our selections
        //  -> this inverse relation is the only reason for making this so complicated
        if (navigationStack.lastOrNull() == DCQLPresentationBuilderGraphViewNavigationAction.PopSelectionsUntilConfirmationInclusive && selection == DCQLPresentationBuilderGraphViewModelSelection.ContinueWithSelection) {
            navigationStack.removeLast()
        } else if(navigationStack.lastOrNull() != selection) {
            navigationStack.add(DCQLPresentationBuilderGraphViewNavigationAction.PushSelection(selection))
        }
    }

    fun popSelection() {
        navigationStack.add(DCQLPresentationBuilderGraphViewNavigationAction.PopSelection)
    }

    fun popSelectionsUntilConfirmationInclusive(onNoSuchElement: () -> Unit) {
        if(selectionStack.any { it is DCQLPresentationBuilderGraphViewModelSelection.ContinueWithSelection }) {
            navigationStack.add(DCQLPresentationBuilderGraphViewNavigationAction.PopSelectionsUntilConfirmationInclusive)
        } else {
            onNoSuchElement()
        }
    }

    companion object {
        val Saver = listSaver(
            save = { state ->
                state.navigationStack.toList().map { navigationAction ->
                    DCQLPresentationBuilderGraphViewNavigationAction.Saver.run {
                        save(navigationAction)!!.also {
                            Napier.d("Saving entry $navigationAction as $it")
                        }
                    }
                }
            },
            restore = { restoredStack ->
                DCQLPresentationBuilderGraphViewNavigationManager(restoredStack.map {
                    DCQLPresentationBuilderGraphViewNavigationAction.Saver.restore(it)!!.also { navigationAction ->
                        Napier.d("Restoring entry $it as $navigationAction")
                    }
                })
            }
        )
    }
}


package ui.presentation

import androidx.compose.runtime.saveable.listSaver

sealed interface DCQLPresentationBuilderGraphViewNavigationAction {
    data class PushSelection(
        val action: DCQLPresentationBuilderGraphViewModelSelection
    ) : DCQLPresentationBuilderGraphViewNavigationAction

    data object PopSelection : DCQLPresentationBuilderGraphViewNavigationAction

    data object PopSelectionsUntilConfirmationInclusive : DCQLPresentationBuilderGraphViewNavigationAction

    companion object {
        val Saver = listSaver(
            save = { action ->
                when (action) {
                    PopSelection -> listOf(0)
                    PopSelectionsUntilConfirmationInclusive -> listOf(1)
                    is PushSelection -> listOf(
                        2,
                        DCQLPresentationBuilderGraphViewModelSelection.Saver.run {
                            save(action.action)!!
                        }
                    )
                }
            },
            restore = { data ->
                when (data[0] as Int) {
                    0 -> PopSelection
                    1 -> PopSelectionsUntilConfirmationInclusive
                    2 -> PushSelection(
                        DCQLPresentationBuilderGraphViewModelSelection.Saver.restore(data[1]) as DCQLPresentationBuilderGraphViewModelSelection
                    )

                    else -> error("Unknown NavigationAction")
                }
            }
        )
    }
}
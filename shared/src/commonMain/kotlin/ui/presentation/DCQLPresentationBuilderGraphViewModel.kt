package ui.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class DCQLPresentationBuilderGraphViewModel : ViewModel() {
    val selectionStack = mutableStateListOf<DCQLPresentationBuilderGraphViewModelAction>()
}
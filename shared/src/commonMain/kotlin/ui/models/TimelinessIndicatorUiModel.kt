package ui.models

interface TimelinessIndicatorUiModel {
    val isExpired: Boolean
    val isNotYetValid: Boolean
    val isTimely: Boolean
        get() = !isNotYetValid && !isExpired
}
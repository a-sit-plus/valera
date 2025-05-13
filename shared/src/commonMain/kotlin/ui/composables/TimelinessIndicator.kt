package ui.composables

interface TimelinessIndicator {
    val isExpired: Boolean
    val isNotYetValid: Boolean
    val isTimely: Boolean
        get() = !isNotYetValid && !isExpired
}
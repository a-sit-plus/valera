import org.jetbrains.compose.resources.StringResource

object AppResetRequiredException : Throwable(message = "AppResetRequiredException") {
    override fun toString() = "AppResetRequiredException"
}

/**
 * Error wrapper used by the UI error flow.
 *
 * It combines two concerns:
 * - deferred side effects via [onAcknowledge] (for example, sending a DCAPI response after user acknowledgement),
 * - optional error-screen action override via [resetStackOverride] + [actionDescriptionOverride].
 *
 * If both override fields are provided, the error screen uses the custom action and label.
 * Otherwise, it falls back to the default error-screen behaviour.
 */
class ErrorHandlingOverrideException(
    /** Optional custom action for the error screen button. */
    val resetStackOverride: (() -> Unit)? = null,
    /** Localised button label used with [resetStackOverride]. */
    val actionDescriptionOverride: StringResource? = null,
    /** Optional side effect executed when the user acknowledges the error. */
    val onAcknowledge: (() -> Unit)? = null,
    override val cause: Throwable?
) : Throwable(message = cause?.message, cause = cause) {
    /** `true` when this exception fully defines a custom error-screen action. */
    val hasUiOverride: Boolean
        get() = resetStackOverride != null && actionDescriptionOverride != null
}

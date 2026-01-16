class UncorrectableErrorException(override val cause: Throwable?) : Throwable(cause = cause)

object AppResetRequiredException : Throwable(message = "AppResetRequiredException") {
    override fun toString() = "AppResetRequiredException"
}

class DeferredErrorActionException(
    val onAcknowledge: () -> Unit,
    override val cause: Throwable?
) : Throwable(message = cause?.message, cause = cause)

class ErrorHandlingOverrideException(
    val resetStackOverride: () -> Unit,
    val actionDescriptionOverride: org.jetbrains.compose.resources.StringResource,
    val onAcknowledge: (() -> Unit)?,
    val onlyForIntentActivity: Boolean,
    override val cause: Throwable?
) : Throwable(message = cause?.message, cause = cause)

class UncorrectableErrorException(override val cause: Throwable?) : Throwable(cause = cause)

object AppResetRequiredException : Throwable(message = "AppResetRequiredException") {
    override fun toString() = "AppResetRequiredException"
}

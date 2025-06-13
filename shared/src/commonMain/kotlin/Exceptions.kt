class UncorrectableErrorException(override val cause: Throwable?) : Throwable(cause = cause)

object AppResetRequiredException : Throwable("App needs reset due to major changes.") {
    override fun toString() = "AppResetRequiredException::class"
}
class UncorrectableErrorException(override val cause: Throwable?): Throwable(message = "UncorrectableErrorException", cause = cause) {
}
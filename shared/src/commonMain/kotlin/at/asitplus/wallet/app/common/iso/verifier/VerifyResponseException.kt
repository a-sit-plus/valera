package at.asitplus.wallet.app.common.iso.verifier

/**
 * Thrown when the verification of a parsed DeviceResponse fails due to structural or validation issues.
 *
 * @param message A human-readable message explaining the error.
 * @param cause The original exception that caused this error.
 */
class VerifyResponseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

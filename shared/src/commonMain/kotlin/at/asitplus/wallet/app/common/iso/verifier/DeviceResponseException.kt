package at.asitplus.wallet.app.common.iso.verifier

/**
 * Represents an exception that occurred while decoding a DeviceResponse.
 *
 * @param message A human-readable message explaining the error.
 * @param cause The original exception that caused this error.
 * @param rawBytes The raw byte array that triggered the failure.
 */
class DeviceResponseException(
    message: String,
    cause: Throwable? = null,
    val rawBytes: ByteArray
) : Exception(message, cause)

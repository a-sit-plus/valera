package at.asitplus.wallet.app.common.dcapi

/**
 * Whether the app may register credentials with the platform's Digital Credentials API.
 */
enum class DigitalCredentialsPermission {
    GRANTED,
    DENIED,

    /** The user has not decided yet, the next registration shows the system prompt. */
    NOT_DETERMINED,

    /** The platform or device does not support registering credentials. */
    NOT_SUPPORTED,
}

/**
 * Outcome of registering credentials with the platform's Digital Credentials API.
 */
enum class DCAPIRegistrationResult {
    /** No credential was left unregistered because of a missing permission. */
    OK,

    /** At least one credential could not be registered because the user denied access. */
    PERMISSION_DENIED,
}

package at.asitplus.wallet.app.common.presentation

// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp

/**
 * Thrown when presentment was cancelled.
 *
 * @property message message to display.
 */
class PresentmentCanceled(message: String): Exception(message)
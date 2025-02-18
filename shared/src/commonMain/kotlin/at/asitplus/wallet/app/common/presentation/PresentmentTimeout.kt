package at.asitplus.wallet.app.common.presentation

// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp

/**
 * Thrown when timing out waiting for the reader to connect.
 *
 * @property message message to display.
 */
class PresentmentTimeout(message: String): Exception(message)
package at.asitplus.wallet.app.common.presentation

// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp

/**
 * Abstract interface to represent a mechanism used to connect a credential reader
 * with a credential prover.
 */
interface PresentmentMechanism {

    /**
     * Closes down the connection and release all resources.
     */
    fun close()
}

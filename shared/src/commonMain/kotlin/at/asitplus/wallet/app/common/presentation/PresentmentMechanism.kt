package at.asitplus.wallet.app.common.presentation

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

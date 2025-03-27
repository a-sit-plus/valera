package ui.viewmodels.authentication

import at.asitplus.wallet.app.common.presentation.MdocPresenter
import at.asitplus.wallet.app.common.presentation.MdocPresentmentMechanism
import at.asitplus.wallet.app.common.presentation.PresentmentCanceled
import at.asitplus.wallet.app.common.presentation.PresentmentMechanism
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.multipaz.mdoc.sessionencryption.SessionEncryption
import org.multipaz.util.Constants
import ui.viewmodels.authentication.PresentationStateModel.DismissType.CLICK
import ui.viewmodels.authentication.PresentationStateModel.DismissType.DOUBLE_CLICK
import ui.viewmodels.authentication.PresentationStateModel.DismissType.LONG_CLICK
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp

/**
 * A model used for credential presentment.
 *
 * This model implements UX/UI flow related to presentment, including
 *
 * - Allowing the user to cancel at any time, including when the connection is being established.
 * - Querying the user for which document to present, if multiple credentials can satisfy the request.
 * - Showing a consent dialog.
 * - Generating the response and sending it to the verifier, via the selected mechanism.
 * - Support for multiple requests if both sides keep the connection open
 */
class PresentationStateModel {
    /**
     * Possible states that the model can be in.
     */
    enum class State {
        /**
         * Presentment is not active.
         */
        IDLE,

        /**
         * Presentment is being initialised.
         */
        INITIALISING,

        /**
         * Check if all permissions are granted.
         */
        CHECK_PERMISSIONS,

        /**
         * Presentment has been started but the mechanism used to communicate with the reader is not yet available.
         */
        CONNECTING,

        /**
         * Presentment is ready, waiting for .
         */
        WAITING_FOR_SOURCE,

        /**
         * Presentment is currently underway.
         */
        PROCESSING,

        /**
         * A request has been received and multiple documents can be presented and the user needs to pick one.
         */
        WAITING_FOR_DOCUMENT_SELECTION,

        /**
         * Presentment is complete. If something went wrong the [error] property is set.
         */
        COMPLETED,

        /**
         * Missing permissions
         */
        NO_PERMISSION
    }

    private val _state = MutableStateFlow(State.IDLE)

    /**
     * The current state.
     */
    val state = _state.asStateFlow()

    private var _presentmentScope: CoroutineScope? = null

    /**
     * A [CoroutineScope] for the presentment process.
     *
     * Any coroutine launched in this scope will be automatically canceled when presentment completes.
     *
     * This should only be read in states which aren't [State.IDLE] and [State.COMPLETED]. It will throw
     * [IllegalStateException] if this is not the case.
     */
    val presentmentScope: CoroutineScope
        get() {
            check(_presentmentScope != null)
            check(_state.value != State.IDLE && _state.value != State.COMPLETED)
            return _presentmentScope!!
        }

    private var _mechanism: PresentmentMechanism? = null

    /**
     * The mechanism being used to communicate with the credential reader.
     */
    private val mechanism: PresentmentMechanism?
        get() = _mechanism

    private var _error: Throwable? = null

    /**
     * If presentment fails, this will be set with a [Throwable] with more information about the failure.
     */
    val error: Throwable?
        get() = _error

    /**
     * Resets the model to [State.IDLE].
     */
    fun reset() {
        _mechanism?.close()
        _mechanism = null
        _error = null
        _dismissible.value = true
        _numRequestsServed.value = 0
        _presentmentScope?.cancel(CancellationException("PresentationModel reset"))
        _presentmentScope = null
        _state.value = State.IDLE
    }

    fun init() {
        check(_state.value == State.IDLE)
        _presentmentScope = CoroutineScope(Dispatchers.Main)
        _state.value = State.INITIALISING
    }

    /**
     * Sets the model to [State.CHECK_PERMISSIONS] if Bluetooth is required or [State.CONNECTING].
     */
    fun start(needBluetooth: Boolean) {
        check(_state.value == State.INITIALISING)
        if (needBluetooth) {
            _state.value = State.CHECK_PERMISSIONS
        } else {
            _state.value = State.CONNECTING
        }
    }


    /**
     * Sets the model to [State.CONNECTING].
     */
    private fun setConnecting() {
        check(_state.value == State.CHECK_PERMISSIONS)
        _state.value = State.CONNECTING
    }

    /**
     * Sets the [PresentmentMechanism] to use.
     *
     * This sets the model to [State.WAITING_FOR_SOURCE].
     *
     * @param mechanism the [PresentmentMechanism] to use.
     */
    fun setMechanism(mechanism: PresentmentMechanism) {
        check(_state.value == State.CONNECTING)
        _mechanism = mechanism
        _state.value = State.WAITING_FOR_SOURCE
    }

    /**
     *
     * This sets the model to [State.PROCESSING].
     *
     * @param presentationViewModel the [PresentationViewModel] to use.
     */
    fun setStepAfterWaitingForSource(presentationViewModel: PresentationViewModel) {
        check(_state.value == State.WAITING_FOR_SOURCE)
        _state.value = State.PROCESSING

        // OK, now that we got both a mechanism and a source we're off to the races and we can
        // start the presentment flow! Do this in a separate coroutine.
        //
        _presentmentScope!!.launch {
            startPresentmentFlow(presentationViewModel)
        }
    }

    /**
     * Sets the model to [State.COMPLETED]
     *
     * @param error pass a [Throwable] if the presentation failed, `null` if successful.
     */
    fun setCompleted(error: Throwable? = null) {
        if (_state.value == State.COMPLETED) {
            Napier.w("Already completed, ignoring second call")
            return
        }
        _mechanism?.close()
        _mechanism = null
        _error = error
        _state.value = State.COMPLETED
        // TODO: Hack to ensure that [state] collectors (using [presentationScope]) gets called for State.COMPLETED
        _presentmentScope?.launch {
            delay(1.seconds)
            _presentmentScope?.cancel(CancellationException("PresentationModel completed"))
            _presentmentScope = null
        }
    }

    /**
     * Three different ways the close/dismiss button can be triggered.
     *
     * This is used by [MdocPresentmentMechanism] to perform either normal session-termination (normal click),
     * transport-specific session termination (long press), or termination without notifying the other end (double
     * click).
     *
     * @property CLICK the user clicked the button normally.
     * @property LONG_CLICK the user performed a long press on the button.
     * @property DOUBLE_CLICK the user double-clicked the button.
     */
    enum class DismissType {
        CLICK,
        LONG_CLICK,
        DOUBLE_CLICK,
    }

    private var _dismissible = MutableStateFlow(true)

    /**
     * Returns whether the presentment can be dismissed/canceled.
     *
     * If this is true the UI layer should include e.g. a button the user can press to dismiss/cancel and
     * call [dismiss] when the user clicks the button.
     */
    val dismissible = _dismissible.asStateFlow()

    private var _numRequestsServed = MutableStateFlow(0)

    /**
     * Number of requests served.
     */
    val numRequestsServed = _numRequestsServed.asStateFlow()

    /**
     * Should be called by the UI layer if the user hits the dismiss button.
     *
     * This ends the presentment flow by calling [setCompleted] with the error parameter set to
     * a [PresentmentCanceled] instance.
     *
     * @param dismissType the type of interaction the user had with the dismiss button
     */
    fun dismiss(dismissType: DismissType) {
        val mdocMechanism = mechanism as? MdocPresentmentMechanism
        if (mdocMechanism != null) {
            _presentmentScope!!.launch {
                try {
                    when (dismissType) {
                        DismissType.CLICK -> {
                            mdocMechanism.transport.sendMessage(
                                SessionEncryption.encodeStatus(Constants.SESSION_DATA_STATUS_SESSION_TERMINATION)
                            )
                            mdocMechanism.transport.close()
                        }

                        DismissType.LONG_CLICK -> {
                            mdocMechanism.transport.sendMessage(byteArrayOf())
                            mdocMechanism.transport.close()
                        }

                        DismissType.DOUBLE_CLICK -> {
                            mdocMechanism.transport.close()
                        }
                    }
                } catch (error: Throwable) {
                    Napier.e("Caught exception closing transport", error)
                    error.printStackTrace()
                }
            }
        }
        setCompleted(PresentmentCanceled("The presentment was canceled by the user"))
    }

    private suspend fun startPresentmentFlow(presentationViewModel: PresentationViewModel) {
        when (mechanism!!) {
            is MdocPresentmentMechanism -> {
                Napier.i("Presenting an mdoc")
                MdocPresenter(
                    stateModel = this,
                    presentationViewModel = presentationViewModel,
                    mechanism = mechanism as MdocPresentmentMechanism
                ).present(
                    dismissible = _dismissible,
                    numRequestsServed = _numRequestsServed,
                    credentialSelected = ::credentialSelected
                )
            }

            else -> throw IllegalStateException("Unsupported mechanism $mechanism")
        }
    }

    private var credentialSelectorContinuation: CancellableContinuation<ByteArray>? = null

    fun setPermissionState(granted: Boolean) {
        check(_state.value == State.CHECK_PERMISSIONS)
        if (!granted) {
            _state.value = State.NO_PERMISSION
        } else {
            setConnecting()
        }
    }

    suspend fun requestCredentialSelection(): ByteArray {
        _state.value = State.WAITING_FOR_DOCUMENT_SELECTION
        return suspendCancellableCoroutine { continuation ->
            credentialSelectorContinuation = continuation
        }
    }

    /**
     * The UI layer should call this when a user has selected a document.
     *
     * This can only be called in state [State.WAITING_FOR_DOCUMENT_SELECTION]
     *
     * @param deviceResponse the device response for the selected credential, must be `null` to convey the user did not want to continue
     */
    fun credentialSelected(deviceResponse: ByteArray) {
        check(_state.value == State.WAITING_FOR_DOCUMENT_SELECTION)
        credentialSelectorContinuation!!.resume(deviceResponse)
    }
}
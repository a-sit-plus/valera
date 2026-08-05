package ui.views.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_select_data
import at.asitplus.valera.resources.info_text_holder_connecting_to_verifier
import at.asitplus.valera.resources.presentation_connecting_to_verifier
import at.asitplus.valera.resources.presentation_initialised
import at.asitplus.valera.resources.presentation_missing_permission
import at.asitplus.valera.resources.presentation_permission_required
import at.asitplus.valera.resources.presentation_waiting_for_request
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.wallet.app.common.presentation.MdocPresentmentMechanism
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalMatchingResult
import at.asitplus.wallet.lib.openid.DCQLMatchingResult
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import ui.composables.buttons.CancelButton
import ui.viewmodels.authentication.AuthenticationConsentViewModel
import ui.viewmodels.authentication.AuthenticationNoCredentialViewModel
import ui.viewmodels.authentication.AuthenticationSelectionPresentationExchangeViewModel
import ui.viewmodels.authentication.AuthenticationViewState
import ui.viewmodels.authentication.PresentationStateModel
import ui.viewmodels.authentication.PresentationViewModel
import ui.views.LoadingView
import ui.views.authentication.AuthenticationConsentView
import ui.views.authentication.AuthenticationNoCredentialView
import ui.views.authentication.AuthenticationSelectionPresentationExchangeView
import ui.views.authentication.AuthenticationSelectionViewScaffold
import kotlin.time.Duration.Companion.seconds

// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp

/**
 * A composable used for credential presentment.
 *
 * Applications should embed this composable wherever credential presentment is required. It communicates with the
 * verifier using [MdocPresentmentMechanism] and [PresentationStateModel].
 *
 * @param presentationViewModel the [PresentationViewModel] to use.
 * @param onPresentmentComplete called when the presentment is complete.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresentationView(
    presentationViewModel: PresentationViewModel,
    onPresentmentComplete: () -> Unit,
    coroutineScope: CoroutineScope,
    snackbarService: SnackbarService,
    onError: (Throwable) -> Unit,
    trustListService: TrustListService
) {
    val presentationStateModel = presentationViewModel.presentationStateModel
    DisposableEffect(presentationViewModel) {
        val previousOnUnauthenticated = presentationViewModel.walletMain.keyMaterial.onUnauthenticated
        presentationViewModel.walletMain.keyMaterial.onUnauthenticated = {}
        onDispose {
            presentationViewModel.walletMain.keyMaterial.onUnauthenticated = previousOnUnauthenticated
        }
    }

    val blePermissionState = rememberBluetoothPermissionState()

    val state = presentationStateModel.state.collectAsState().value
    val dismissible = presentationStateModel.dismissible.collectAsState().value
    val cancelPresentment = {
        presentationStateModel.dismiss(PresentationStateModel.DismissType.CLICK)
    }
    val loadingAction: (@Composable () -> Unit)? =
        if (dismissible && state != PresentationStateModel.State.COMPLETED) {
            { CancelButton(onClick = cancelPresentment) }
        } else {
            null
        }
    when (state) {
        PresentationStateModel.State.IDLE,
        PresentationStateModel.State.NO_PERMISSION,
        PresentationStateModel.State.INITIALISING,
        PresentationStateModel.State.CONNECTING -> {}

        PresentationStateModel.State.CHECK_PERMISSIONS -> {
            if (blePermissionState.isGranted) {
                presentationStateModel.setPermissionState(true)
            } else {
                coroutineScope.launch {
                    blePermissionState.launchPermissionRequest()
                }
            }
        }

        PresentationStateModel.State.WAITING_FOR_SOURCE -> {
            presentationStateModel.setStepAfterWaitingForSource(presentationViewModel)
        }

        PresentationStateModel.State.PROCESSING -> {}
        PresentationStateModel.State.WAITING_FOR_DOCUMENT_SELECTION -> {
            when (presentationViewModel.viewState) {
                AuthenticationViewState.Consent -> {
                    AuthenticationConsentView(
                        vm = AuthenticationConsentViewModel(
                            spName = presentationViewModel.spName,
                            spLocation = presentationViewModel.spLocation,
                            spImage = presentationViewModel.spImage,
                            transactionData = presentationViewModel.transactionData,
                            navigateUp = presentationViewModel.navigateUp,
                            onCancel = { presentationViewModel.onCancel() },
                            buttonConsent = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    presentationViewModel.onConsent()
                                }
                            },
                            walletMain = presentationViewModel.walletMain,
                            presentationRequest = presentationViewModel.presentationRequest,
                            onClickLogo = presentationViewModel.onClickLogo,
                            onUnauthenticated = {}
                        ),
                        onError = onError
                    )
                }

                AuthenticationViewState.NoMatchingCredential -> {
                    AuthenticationNoCredentialView(AuthenticationNoCredentialViewModel(
                            navigateToHomeScreen = presentationViewModel.navigateToHomeScreen
                        )
                    )
                }

                AuthenticationViewState.Selection -> {
                    when (val matching = presentationViewModel.matchingCredentials) {
                        is at.asitplus.wallet.lib.agent.DCQLMatchingResult -> {
                            AuthenticationSelectionViewScaffold(
                                title = stringResource(Res.string.heading_label_select_data),
                                onNavigateUp = presentationViewModel.navigateUp,
                                onClickLogo = presentationViewModel.onClickLogo,
                                onNext = {
                                    presentationViewModel.confirmSelection(null)
                                },
                            ) {
                                Column {
                                    // TODO: make string resources
                                    Text("Implementation of DCQL Query Credential Selection is in progress.")
                                    Text("Click continue to submit the credentials that are selected by default.")
                                }
                            }
                        }

                        is at.asitplus.wallet.lib.agent.PresentationExchangeMatchingResult -> {
                            AuthenticationSelectionPresentationExchangeView(
                                vm = AuthenticationSelectionPresentationExchangeViewModel(
                                    confirmSelections = { selections ->
                                        presentationViewModel.confirmSelection(selections)
                                    },
                                    navigateUp = { presentationViewModel.viewState = AuthenticationViewState.Consent },
                                    credentialMatchingResult = matching
                                ),
                                onError = onError,
                                onClickLogo = presentationViewModel.onClickLogo,
                                trustListService = trustListService
                            )
                        }

                        is IsoDeviceRetrievalMatchingResult<*> -> TODO()
                    }
                }
            }
        }

        PresentationStateModel.State.COMPLETED -> {
            // LaunchedEffect(Unit) fires once per composition entry, preventing repeated
            // calls to onPresentmentComplete/onError when the composable recomposes.
            LaunchedEffect(Unit) {
                when (val error = presentationStateModel.error) {
                    null -> {
                        delay(3.seconds)
                        onPresentmentComplete()
                    }
                    else -> onError(error)
                }
            }
        }
    }

    if (state != PresentationStateModel.State.WAITING_FOR_DOCUMENT_SELECTION) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            when (state) {
                PresentationStateModel.State.IDLE,
                PresentationStateModel.State.CONNECTING ->
                    LoadingView(
                        customLabel = stringResource(Res.string.presentation_connecting_to_verifier),
                        action = loadingAction
                    )

                PresentationStateModel.State.WAITING_FOR_SOURCE,
                PresentationStateModel.State.PROCESSING -> LoadingView(
                    customLabel = if (presentationStateModel.numRequestsServed.collectAsState().value == 0) {
                        stringResource(Res.string.info_text_holder_connecting_to_verifier)
                    } else {
                        stringResource(Res.string.presentation_waiting_for_request)
                    },
                    action = loadingAction
                )

                PresentationStateModel.State.COMPLETED ->
                    if (presentationStateModel.error == null) {
                        PresentationCompletedView(null)
                    }
                    // Error case: render nothing. onError() is already in flight from the
                    // coroutineScope.launch above and will navigate to the error screen immediately.

                PresentationStateModel.State.WAITING_FOR_DOCUMENT_SELECTION ->
                    throw IllegalStateException("should not be reachable")

                PresentationStateModel.State.NO_PERMISSION ->
                    LoadingView(
                        customLabel = stringResource(Res.string.presentation_missing_permission),
                        action = loadingAction
                    )

                PresentationStateModel.State.CHECK_PERMISSIONS ->
                    LoadingView(
                        customLabel = stringResource(Res.string.presentation_permission_required),
                        action = loadingAction
                    )

                PresentationStateModel.State.INITIALISING ->
                    LoadingView(
                        customLabel = stringResource(Res.string.presentation_initialised),
                        action = loadingAction
                    )
            }
        }
    }

    // We show a X in the top-right to resemble a close button, under two circumstances
    //
    // - when connecting the the remote reader, because the underlying connection via NFC / BLE
    //   could hang and/or take a long time. This gives the user an opportunity to stop the
    //   transaction. Only applicable for for proximity.
    //
    // - in the case where the connection is kept alive and we're waiting for a second request from
    //   the reader. This also only applies to proximity and in this case we have a bit of
    //   hidden developer functionality insofar that if long-pressing we'll use session-specific
    //   termination (according to 18013-5) and if double-clicking we'll close the connection without
    //   sending a termination message at all. This is useful for testing and at interoperability events
    //   and since it's hidden it doesn't materially affect a production app.
    //
    if (dismissible && state != PresentationStateModel.State.COMPLETED) {
        // TODO: for phones with display cutouts in the top-right (for example Pixel 9 Pro Fold when unfolded)
        //   the Close icon may be obscured. Examine the displayCutouts path and move the icon so it doesn't
        //   overlap.
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd).padding(20.dp)
                    .combinedClickable(
                        onClick = cancelPresentment,
                        onLongClick = { presentationStateModel.dismiss(PresentationStateModel.DismissType.LONG_CLICK) },
                        onDoubleClick = { presentationStateModel.dismiss(PresentationStateModel.DismissType.DOUBLE_CLICK) },
                    ),
            )
        }
    }
}

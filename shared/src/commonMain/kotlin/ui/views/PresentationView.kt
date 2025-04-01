package ui.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.app_display_name
import at.asitplus.valera.resources.icon_presentation
import at.asitplus.valera.resources.icon_presentation_error
import at.asitplus.valera.resources.icon_presentation_success
import at.asitplus.valera.resources.presentation_canceled
import at.asitplus.valera.resources.presentation_connecting_to_verifier
import at.asitplus.valera.resources.presentation_error
import at.asitplus.valera.resources.presentation_success
import at.asitplus.valera.resources.presentation_timeout
import at.asitplus.valera.resources.presentation_waiting_for_request
import at.asitplus.valera.resources.presentation_initialised
import at.asitplus.valera.resources.presentation_missing_permission
import at.asitplus.valera.resources.presentation_permission_required
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.presentation.PresentmentCanceled
import ui.viewmodels.authentication.PresentationStateModel
import at.asitplus.wallet.app.common.presentation.PresentmentTimeout
import at.asitplus.wallet.app.permissions.RequestBluetoothPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.viewmodels.authentication.AuthenticationConsentViewModel
import ui.viewmodels.authentication.AuthenticationNoCredentialViewModel
import ui.viewmodels.authentication.AuthenticationSelectionPresentationExchangeViewModel
import ui.viewmodels.authentication.AuthenticationViewState
import ui.viewmodels.authentication.DCQLMatchingResult
import ui.viewmodels.authentication.PresentationExchangeMatchingResult
import ui.viewmodels.authentication.PresentationViewModel
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
 * verifier using [PresentmentMechanism] and [PresentationStateModel] and gets application-specific data sources and
 * policy using [PresentmentSource].
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
    onError: (Throwable) -> Unit
) {
    val presentationStateModel = presentationViewModel.presentationStateModel
    presentationViewModel.walletMain.cryptoService.onUnauthenticated =
        presentationViewModel.navigateUp


    // Make sure we clean up the PresentmentModel when we're done. This is to ensure
    // the mechanism is properly shut down, for example for proximity we need to release
    // all BLE and NFC resources.
    //
    DisposableEffect(presentationStateModel) {
        onDispose {
            presentationStateModel.reset()
        }
    }

    val state = presentationStateModel.state.collectAsState().value
    when (state) {
        PresentationStateModel.State.IDLE,
        PresentationStateModel.State.NO_PERMISSION,
        PresentationStateModel.State.INITIALISING,
        PresentationStateModel.State.CONNECTING -> {
        }

        PresentationStateModel.State.CHECK_PERMISSIONS ->
            RequestBluetoothPermissions({ granted ->
                presentationStateModel.setPermissionState(
                    granted
                )
            }, snackbarService::showSnackbar)

        PresentationStateModel.State.WAITING_FOR_SOURCE -> {
            presentationStateModel.setStepAfterWaitingForSource(presentationViewModel)
        }

        PresentationStateModel.State.PROCESSING -> {}
        PresentationStateModel.State.WAITING_FOR_DOCUMENT_SELECTION -> {
            when (presentationViewModel.viewState) {
                AuthenticationViewState.Consent -> {
                    val viewModel = AuthenticationConsentViewModel(
                        spName = presentationViewModel.spName,
                        spLocation = presentationViewModel.spLocation,
                        spImage = presentationViewModel.spImage,
                        transactionData = presentationViewModel.transactionData,
                        navigateUp = presentationViewModel.navigateUp,
                        buttonConsent = {
                            CoroutineScope(Dispatchers.IO).launch {
                                presentationViewModel.onConsent()
                            }
                        },
                        walletMain = presentationViewModel.walletMain,
                        presentationRequest = presentationViewModel.presentationRequest,
                        onClickLogo = presentationViewModel.onClickLogo
                    )
                    AuthenticationConsentView(
                        viewModel,
                        onError = onError,
                    )
                }

                AuthenticationViewState.NoMatchingCredential -> {
                    val viewModel =
                        AuthenticationNoCredentialViewModel(navigateToHomeScreen = presentationViewModel.navigateToHomeScreen)
                    AuthenticationNoCredentialView(vm = viewModel)
                }

                AuthenticationViewState.Selection -> {
                    when(val matching = presentationViewModel.matchingCredentials) {
                        is DCQLMatchingResult -> {
                            AuthenticationSelectionViewScaffold(
                                onNavigateUp = presentationViewModel.navigateUp,
                                onClickLogo = {},
                                onNext = {
                                    presentationViewModel.confirmSelection(null)
                                },
                            ) {
                                Column {
                                    Text("Implementation of DCQL Query Credential Selection is in progress.")
                                    Text("Click continue to submit the credentials that are selected by default.")
                                }
                            }
                        }

                        is PresentationExchangeMatchingResult -> {
                            val viewModel = AuthenticationSelectionPresentationExchangeViewModel(
                                walletMain = presentationViewModel.walletMain,
                                confirmSelections = { selections ->
                                    presentationViewModel.confirmSelection(selections)
                                },
                                navigateUp = { presentationViewModel.viewState = AuthenticationViewState.Consent },
                                onClickLogo = presentationViewModel.onClickLogo,
                                credentialMatchingResult = matching,
                            )
                            AuthenticationSelectionPresentationExchangeView(vm = viewModel)
                        }
                    }
                }
            }
        }

        PresentationStateModel.State.COMPLETED -> {
            // Delay for a short amount of time so the user has a chance to see the success/error indication
            coroutineScope.launch {
                delay(1.5.seconds)
                onPresentmentComplete()
            }
        }
    }

    if (state != PresentationStateModel.State.WAITING_FOR_DOCUMENT_SELECTION) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.15f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (appNameText, iconPainter, iconCaptionText) = when (state) {
                    PresentationStateModel.State.IDLE,
                    PresentationStateModel.State.CONNECTING -> {
                        Triple(
                            stringResource(Res.string.app_display_name),
                            painterResource(Res.drawable.icon_presentation),
                            stringResource(Res.string.presentation_connecting_to_verifier)
                        )
                    }

                    PresentationStateModel.State.WAITING_FOR_SOURCE,
                    PresentationStateModel.State.PROCESSING
                        -> {
                        Triple(
                            stringResource(Res.string.app_display_name),
                            painterResource(Res.drawable.icon_presentation),
                            if (presentationStateModel.numRequestsServed.collectAsState().value == 0) {
                                ""
                            } else {
                                stringResource(Res.string.presentation_waiting_for_request)
                            }
                        )
                    }

                    PresentationStateModel.State.COMPLETED -> {
                        when (presentationStateModel.error) {
                            null -> Triple(
                                "",
                                painterResource(Res.drawable.icon_presentation_success),
                                stringResource(Res.string.presentation_success)
                            )

                            is PresentmentCanceled -> Triple(
                                stringResource(Res.string.app_display_name),
                                painterResource(Res.drawable.icon_presentation),
                                stringResource(Res.string.presentation_canceled)
                            )

                            is PresentmentTimeout -> Triple(
                                "",
                                painterResource(Res.drawable.icon_presentation_error),
                                stringResource(Res.string.presentation_timeout)
                            )

                            else -> Triple(
                                "",
                                painterResource(Res.drawable.icon_presentation_error),
                                stringResource(Res.string.presentation_error)
                            )
                        }
                    }

                    PresentationStateModel.State.WAITING_FOR_DOCUMENT_SELECTION -> throw IllegalStateException(
                        "should not be reachable"
                    )

                    PresentationStateModel.State.NO_PERMISSION -> Triple(
                        stringResource(Res.string.app_display_name),
                        painterResource(Res.drawable.icon_presentation),
                        stringResource(Res.string.presentation_missing_permission)
                    )

                    PresentationStateModel.State.CHECK_PERMISSIONS -> Triple(
                        stringResource(Res.string.app_display_name),
                        painterResource(Res.drawable.icon_presentation),
                        stringResource(Res.string.presentation_permission_required)

                    )

                    PresentationStateModel.State.INITIALISING -> Triple(
                        stringResource(Res.string.app_display_name),
                        painterResource(Res.drawable.icon_presentation),
                        stringResource(Res.string.presentation_initialised)

                    )
                }


                Text(
                    text = appNameText,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Image(
                    modifier = Modifier.size(200.dp).fillMaxSize().padding(10.dp),
                    painter = iconPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )
                Text(
                    text = iconCaptionText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal
                )


            }
            Spacer(modifier = Modifier.weight(1.0f))
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
    if (presentationStateModel.dismissible.collectAsState().value && state != PresentationStateModel.State.COMPLETED) {
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
                        onClick = { presentationStateModel.dismiss(PresentationStateModel.DismissType.CLICK) },
                        onLongClick = { presentationStateModel.dismiss(PresentationStateModel.DismissType.LONG_CLICK) },
                        onDoubleClick = { presentationStateModel.dismiss(PresentationStateModel.DismissType.DOUBLE_CLICK) },
                    ),
            )
        }
    }
}
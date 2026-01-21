package ui.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope
import ui.navigation.routes.AuthenticationSuccessRoute
import ui.views.authentication.AuthenticationSuccessView

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun DefaultPresentationGraphView(
    onNavigateUp: () -> Unit,
    onError: (Throwable) -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    koinScope: Scope,
    navController: NavHostController = rememberNavController(),
    viewModel: DefaultPresentationGraphViewModel = koinViewModel(scope = koinScope),
) {
    val dcApiRequest = try {
        viewModel.dcApiRequest.getOrThrow()
    } catch (throwable: Throwable) {
        onError(throwable)
        return
    }

    val spLocation = dcApiRequest?.callingOrigin ?: viewModel.route.recipientLocation
    val spName = dcApiRequest?.callingPackageName
    val authenticationRequest = viewModel.route.authenticationRequest
    val transactionData = authenticationRequest.parameters.transactionData?.firstOrNull()

    navController.navigateUp()

    val authenticateAtRelyingParty = spLocation != "Local Presentation"

    NavHost(
        navController = navController,
        startDestination = PresentationStartRoute::class,
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
            )
        },
    ) {
        composable<PresentationStartRoute> {
            CommonPresentationPageScaffold(
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
                onNavigateUp = onNavigateUp,
            ) {
                AuthenticationReceivedStartPageContent(
                    authenticateAtRelyingParty = authenticateAtRelyingParty,
                    onContinue = {
                        navController.navigate(PresentationBuilderGraphRoute)
                    },
                    serviceProviderLogo = null,
                    serviceProviderLocalizedName = spName,
                    serviceProviderLocalizedLocation = spLocation,
                    onAbort = onNavigateUp,
                )
            }
        }

        composable<PresentationBuilderGraphRoute> {
            val matchingResult by viewModel.matchingResult.collectAsState()
            LaunchedEffect(matchingResult) {
                matchingResult.let {
                    if (it is UiStateError) {
                        onError(it.throwable)
                    }
                }
            }
            PresentationBuilderGraphView(
                authenticateAtRelyingParty = authenticateAtRelyingParty,
                serviceProviderLocalizedName = spName,
                serviceProviderLocalizedLocation = spLocation,
                matchingResult = matchingResult,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
                onError = onError,
                onNavigateToPresentationStart = {
                    navController.popBackStack(
                        inclusive = false,
                        route = PresentationStartRoute,
                    )
                },
                onSubmit = {
                    viewModel.confirmSelection(
                        credentialPresentationSubmissions = it,
                        onFailure = {
                            onError(it)
                        },
                        onSuccess = {
                            when(it) {
                                is OpenId4VpWallet.AuthenticationForward -> {
                                    onError(UnsupportedOperationException("Unexpected authentication result `forward`"))
                                }

                                is OpenId4VpWallet.AuthenticationSuccess -> navController.navigate(
                                    AuthenticationSuccessRoute(
                                        redirectUrl = it.redirectUri,
                                        isCrossDeviceFlow = viewModel.route.isCrossDeviceFlow,
                                    )
                                )
                            }
                        }
                    )
                }
            )
        }

        composable<AuthenticationSuccessRoute> {
            AuthenticationSuccessView(
                navigateUp = onNavigateUp,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
                koinScope = koinScope
            )
        }
    }
}
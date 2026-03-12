package ui.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import at.asitplus.openid.TransactionDataBase64Url
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.openid.CredentialMatchingResult
import ui.views.authentication.AuthenticationSuccessView
import ui.views.authentication.TransactionDataView

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun PresentationGraphView(
    serviceProviderLogo: ImageBitmap?,
    serviceProviderNameLocalized: String?,
    serviceProviderLocationLocalized: String,
    authenticateAtRelyingParty: Boolean,
    onNavigateUp: () -> Unit,
    onError: (Throwable) -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    matchingResult: UiState<CredentialMatchingResult<SubjectCredentialStore.StoreEntry>>,
    submitPresentation: SubmitPresentation,
    navController: NavHostController = rememberNavController(),
    transactionData: TransactionDataBase64Url?,
    presentationRequest: CredentialPresentationRequest?,
) {
    LaunchedEffect(matchingResult) {
        matchingResult.let {
            if (it is UiStateError) {
                onError(it.throwable)
            }
        }
    }

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
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
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
                    presentationRequest = presentationRequest,
                    authenticateAtRelyingParty = authenticateAtRelyingParty,
                    serviceProviderLogo = serviceProviderLogo,
                    serviceProviderLocalizedName = serviceProviderNameLocalized,
                    serviceProviderLocalizedLocation = serviceProviderLocationLocalized,
                    onAbort = onNavigateUp,
                    onContinue = {
                        navController.navigate(PresentationBuilderGraphRoute) {
                            popUpTo(PresentationStartRoute) {
                                inclusive = false
                            }
                            restoreState = true
                        }
                    },
                    additionalDataView = if(transactionData != null) {
                        {
                            Spacer(modifier = Modifier.height(32.dp))
                            TransactionDataView(transactionData)
                        }
                    } else {
                        null
                    },
                    onError = onError,
                )
            }
        }

        composable<PresentationBuilderGraphRoute> {
            PresentationBuilderGraphView(
                authenticateAtRelyingParty = authenticateAtRelyingParty,
                serviceProviderLocalizedName = serviceProviderNameLocalized,
                serviceProviderLocalizedLocation = serviceProviderLocationLocalized,
                matchingResult = matchingResult,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
                onError = onError,
                onNavigateToPresentationStart = {
                    navController.navigate(PresentationStartRoute) {
                        popUpTo(PresentationStartRoute) {
                            saveState = true
                            inclusive = false
                        }
                        // for some reason this is needed to make the popEnter animation for the start route
                        launchSingleTop = true
                    }
                },
                onSubmit = {
                    submitPresentation(it) {
                        navController.navigate(it) {
                            popUpTo(PresentationStartRoute::class) {
                                inclusive = true
                            }
                        }
                    }
                }
            )
        }

        composable<PresentationSuccessRoute> {
            BackHandler {
                onNavigateUp()
            }
            AuthenticationSuccessView(
                navigateUp = onNavigateUp,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings
            )
        }
    }
}


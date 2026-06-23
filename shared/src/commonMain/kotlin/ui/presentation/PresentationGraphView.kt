package ui.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import at.asitplus.openid.TransactionDataBase64Url
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_authenticate_at_device_title
import at.asitplus.valera.resources.heading_label_select_data
import at.asitplus.valera.resources.heading_label_show_data
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import org.jetbrains.compose.resources.stringResource
import org.koin.core.scope.Scope
import ui.views.authentication.AuthenticationSuccessView
import ui.views.authentication.TransactionDataView

@ExperimentalMaterial3Api
@Composable
fun PresentationGraphView(
    koinScope: Scope,
    serviceProviderLogo: ImageBitmap?,
    serviceProviderNameLocalized: String?,
    serviceProviderLocationLocalized: String,
    authenticateAtRelyingParty: Boolean,
    onNavigateUp: () -> Unit,
    onError: (Throwable) -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    selectionProvider: UiState<CredentialSelectionProvider<SubjectCredentialStore.StoreEntry>>,
    submitPresentation: SubmitPresentation,
    navController: NavHostController = rememberNavController(),
    transactionData: TransactionDataBase64Url?,
    presentationRequest: CredentialPresentationRequest?,
    navigateUpIsClose: Boolean = false,
    showStartRoute: Boolean = true,
) {
    LaunchedEffect(selectionProvider) {
        selectionProvider.let {
            if (it is UiStateError) {
                onError(it.throwable)
            }
        }
    }
    val startPageTitle = if (authenticateAtRelyingParty) {
        stringResource(Res.string.heading_label_authenticate_at_device_title)
    } else {
        stringResource(Res.string.heading_label_show_data)
    }
    val selectionPageTitle = stringResource(Res.string.heading_label_select_data)

    NavHost(
        navController = navController,
        startDestination = if (showStartRoute) PresentationStartRoute::class else PresentationBuilderGraphRoute::class,
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
                title = startPageTitle,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
                onNavigateUp = onNavigateUp,
                navigateUpIsClose = navigateUpIsClose,
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
                title = selectionPageTitle,
                authenticateAtRelyingParty = authenticateAtRelyingParty,
                serviceProviderLocalizedName = serviceProviderNameLocalized,
                serviceProviderLocalizedLocation = serviceProviderLocationLocalized,
                selectionProvider = selectionProvider,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
                onError = onError,
                onNavigateUp = onNavigateUp,
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
            val backState = rememberNavigationEventState(NavigationEventInfo.None)
            NavigationBackHandler(state = backState) { onNavigateUp() }
            AuthenticationSuccessView(
                koinScope = koinScope,
                navigateUp = onNavigateUp,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
                navigateUpIsClose = navigateUpIsClose,
            )
        }
    }
}

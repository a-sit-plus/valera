package ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import composewalletapp.shared.generated.resources.error_feature_not_yet_available
import composewalletapp.shared.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import ui.navigation.NavigationStack
import ui.navigation.OnboardingInformationPage
import ui.navigation.OnboardingPage
import ui.navigation.OnboardingStartPage
import ui.navigation.OnboardingTermsPage
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString

@Composable
fun OnboardingWrapper(
    walletMain: WalletMain,
    content: @Composable () -> Unit,
) {
    val isConditionsAccepted by walletMain.walletConfig.isConditionsAccepted.collectAsState(null)

    when (isConditionsAccepted) {
        null -> {}
        true -> content()
        false -> OnboardingNavigator(
            onOnboardingComplete = {
                walletMain.walletConfig.set(isConditionsAccepted = true)
            },
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun OnboardingNavigator(
    onOnboardingComplete: () -> Unit,
) {
    // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer
    val navigationStack = rememberSaveable(
        saver = listSaver(
            restore = { NavigationStack(*it.toTypedArray()) },
            save = { it.stack },
        )
    ) {
        NavigationStack<OnboardingPage>(OnboardingStartPage())
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService = SnackbarService(CoroutineScope(Dispatchers.Default), snackbarHostState)

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) {
        AnimatedContent(targetState = navigationStack.lastWithIndex()) { (_, page) ->
            when (page) {
                is OnboardingStartPage -> {
                    OnboardingStartScreen(
                        onClickStart = {
                            navigationStack.push(OnboardingInformationPage())
                        }
                    )
                }

                is OnboardingInformationPage -> {
                    OnboardingInformationScreen(
                        onClickContinue = {
                            navigationStack.push(OnboardingTermsPage())
                        }
                    )
                }

                is OnboardingTermsPage -> {
                    OnboardingTermsScreen(
                        onClickNavigateBack = {
                            navigationStack.back()
                        },
                        onClickReadGeneralTermsAndConditions = {
                            runBlocking {
                                snackbarService.showSnackbar(getString(Res.string.error_feature_not_yet_available))
                            }
                        },
                        onClickReadDataProtectionPolicy = {
                            runBlocking {
                                snackbarService.showSnackbar(getString(Res.string.error_feature_not_yet_available))
                            }
                        },
                        onClickAccept = {
                            onOnboardingComplete()
                        },
                    )
                }
            }
        }
    }
}
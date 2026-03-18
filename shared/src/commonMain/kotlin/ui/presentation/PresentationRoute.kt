package ui.presentation

import kotlinx.serialization.Serializable

interface PresentationRoute

sealed interface PresentationResponseRoute : PresentationRoute

@Serializable
data class PresentationSuccessRoute(
    val redirectUrl: String?,
    val isCrossDeviceFlow: Boolean
) : PresentationResponseRoute

@Serializable
data object PresentationBuilderGraphRoute : PresentationRoute

@Serializable
data object PresentationStartRoute : PresentationRoute
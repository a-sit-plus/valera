package ui.presentation

import kotlinx.serialization.Serializable

@Serializable
data class PresentationFinalizedSuccessRoute(val redirectUri: String?) : PresentationRoute
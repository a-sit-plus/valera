package data.dcapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DisplayInfoField(
    @SerialName(TITLE)
    val title: String,
    @SerialName(SUBTITLE)
    val subtitle: String,
    @SerialName(DISCLAIMER)
    val disclaimer: String?,
    @SerialName(WARNING)
    val warning: String?,
    @SerialName(ICON_ID)
    var iconId: Int? = null,
) {

    companion object {
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
        const val DISCLAIMER = "disclaimer"
        const val WARNING = "warning"
        const val ICON_ID = "icon_id"
    }
}
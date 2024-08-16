package data

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.eupid.IsoIec5218Gender
import kotlinx.datetime.LocalDate

sealed interface Attribute {
    companion object {
        fun fromValue(value: String?) = value?.let { StringAttribute(it) }
        fun fromValue(value: IsoIec5218Gender?) = value?.let { GenderAttribute(it) }
        fun fromValue(value: Int?) = value?.let { IntegerAttribute(it) }
        fun fromValue(value: UInt?) = value?.let { UnsignedIntegerAttribute(it) }
        fun fromValue(value: Boolean?) = value?.let { BooleanAttribute(it) }
        fun fromValue(value: LocalDate?) = value?.let { DateAttribute(it) }
        fun fromValue(value: ImageBitmap?) = value?.let { ImageAttribute(it) }
    }

    data class StringAttribute(val value: String) : Attribute
    data class GenderAttribute(val value: IsoIec5218Gender) : Attribute
    data class IntegerAttribute(val value: Int) : Attribute
    data class UnsignedIntegerAttribute(val value: UInt) : Attribute
    data class BooleanAttribute(val value: Boolean) : Attribute
    data class DateAttribute(val value: LocalDate) : Attribute
    data class ImageAttribute(val value: ImageBitmap) : Attribute
}
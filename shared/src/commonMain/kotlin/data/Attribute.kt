package data

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.mdl.DrivingPrivilege
import at.asitplus.wallet.mdl.IsoSexEnum
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

sealed interface Attribute {
    companion object {
        fun fromValue(value: String?) = value?.let { StringAttribute(it) }
        fun fromValue(value: IsoIec5218Gender?) = value?.let { GenderAttribute(it) }
        fun fromValue(value: IsoSexEnum?) = value?.let { SexAttribute(it) }
        fun fromValue(value: Int?) = value?.let { IntegerAttribute(it) }
        fun fromValue(value: UInt?) = value?.let { UnsignedIntegerAttribute(it) }
        fun fromValue(value: Boolean?) = value?.let { BooleanAttribute(it) }
        fun fromValue(value: LocalDate?) = value?.let { DateAttribute(it) }
        fun fromValue(value: LocalDateTime?) = value?.let { DateTimeAttribute(it) }
        fun fromValue(value: Instant?) = value?.let { InstantAttribute(it) }
        fun fromValue(value: ImageBitmap?) = value?.let { ImageAttribute(it) }
        fun fromValue(value: Array<DrivingPrivilege>?) = value?.let { DrivingPrivilegeAttribute(it) }
    }

    data class StringAttribute(val value: String) : Attribute
    data class GenderAttribute(val value: IsoIec5218Gender) : Attribute
    data class SexAttribute(val value: IsoSexEnum) : Attribute
    data class IntegerAttribute(val value: Int) : Attribute
    data class UnsignedIntegerAttribute(val value: UInt) : Attribute
    data class BooleanAttribute(val value: Boolean) : Attribute
    data class DateAttribute(val value: LocalDate) : Attribute
    data class DateTimeAttribute(val value: LocalDateTime) : Attribute
    data class InstantAttribute(val value: Instant) : Attribute
    data class ImageAttribute(val value: ImageBitmap) : Attribute
    data class DrivingPrivilegeAttribute(val value: Array<DrivingPrivilege>) : Attribute
}
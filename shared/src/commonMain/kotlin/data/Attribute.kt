package data

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.companyregistration.Address
import at.asitplus.wallet.companyregistration.Branch
import at.asitplus.wallet.companyregistration.CompanyActivity
import at.asitplus.wallet.companyregistration.ContactData
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.mdl.DrivingPrivilege
import at.asitplus.wallet.mdl.IsoSexEnum
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.longOrNull

sealed interface Attribute {
    companion object {
        fun fromValue(value: Any?): Attribute? = if (value == null) null else when (val it = value) {
            is Array<*> -> fromValueList(it.toList())
            is Collection<*> -> fromValueList(it.toList())
            is JsonNull -> null
            is JsonPrimitive -> if (it.isString) {
                fromValue(it.content)
            } else {
                it.booleanOrNull?.let { fromValue(it) }
                    ?: it.longOrNull?.let { fromValue(it) }
                    ?: it.double.let { fromValue(it) }
            }

            is String -> StringAttribute(it)
            is IsoIec5218Gender -> GenderAttribute(it)
            is IsoSexEnum -> SexAttribute(it)
            is Int -> IntegerAttribute(it)
            is UInt -> UnsignedIntegerAttribute(it)
            is Boolean -> BooleanAttribute(it)
            is LocalDate -> DateAttribute(it)
            is LocalDateTime -> DateTimeAttribute(it)
            is Instant -> InstantAttribute(it)
            is ImageBitmap -> ImageAttribute(it)
            is Long -> LongAttribute(it)
            is CompanyActivity -> CompanyActivityAttribute(it)
            is ContactData -> ContactDataAttribute(it)
            is Address -> AddressAttribute(it)
            is Branch -> BranchAttribute(it)
            else -> throw IllegalArgumentException("Unexpected attribute value type: ${value::class}, $value")
        }

        private fun fromValueList(valueList: List<Any?>) = runCatching {
            StringListAttribute(valueList.map { it as String })
        }.getOrNull() ?: runCatching {
            DrivingPrivilegeAttribute(valueList.map { it as DrivingPrivilege }.toTypedArray())
        }.getOrNull() ?: runCatching {
            StringListAttribute(valueList.map { (it as JsonPrimitive).content })
        }.getOrNull() ?: StringListAttribute(valueList.map { it.toString() })
    }

    data class StringAttribute(val value: String) : Attribute
    data class StringListAttribute(val value: Collection<String>) : Attribute
    data class GenderAttribute(val value: IsoIec5218Gender) : Attribute
    data class SexAttribute(val value: IsoSexEnum) : Attribute
    data class IntegerAttribute(val value: Int) : Attribute
    data class LongAttribute(val value: Long) : Attribute
    data class UnsignedIntegerAttribute(val value: UInt) : Attribute
    data class BooleanAttribute(val value: Boolean) : Attribute
    data class DateAttribute(val value: LocalDate) : Attribute
    data class DateTimeAttribute(val value: LocalDateTime) : Attribute
    data class InstantAttribute(val value: Instant) : Attribute
    data class ImageAttribute(val value: ImageBitmap) : Attribute
    data class DrivingPrivilegeAttribute(val value: Array<DrivingPrivilege>) : Attribute
    data class CompanyActivityAttribute(val value: CompanyActivity) : Attribute
    data class ContactDataAttribute(val value: ContactData) : Attribute
    data class AddressAttribute(val value: Address) : Attribute
    data class BranchAttribute(val value: Branch) : Attribute
}

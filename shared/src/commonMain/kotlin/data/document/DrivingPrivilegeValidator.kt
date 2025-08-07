package data.document

import at.asitplus.wallet.mdl.DrivingPrivilege
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DrivingPrivilegeValidator {
    private val currentDate
        get() = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

    enum class Status {
        VALID,
        EXPIRED,
        NOT_YET_VALID
    }

    fun isDrivingPrivilegeExpired(drivingPrivilege: DrivingPrivilege): Boolean? =
        drivingPrivilege.expiryDate?.let { currentDate > it }

    fun isDrivingPrivilegeNotYetValid(drivingPrivilege: DrivingPrivilege): Boolean? =
        drivingPrivilege.issueDate?.let { currentDate < it }

    fun getDrivingPrivilegeStatus(drivingPrivilege: DrivingPrivilege): Status {
        if (isDrivingPrivilegeExpired(drivingPrivilege) == true) return Status.EXPIRED
        if (isDrivingPrivilegeNotYetValid(drivingPrivilege) == true) return Status.NOT_YET_VALID
        return Status.VALID
    }
}

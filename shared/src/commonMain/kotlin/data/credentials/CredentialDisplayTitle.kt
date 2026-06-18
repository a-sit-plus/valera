package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

fun SubjectCredentialStore.StoreEntry.displayTitle(schemeLabel: String): String {
    val detail = catchingUnwrapped {
        scheme.let { s ->
            when {
                s.isEuPid -> EuPidCredentialAdapter.createFromStoreEntry(this, failingImageDecoder)
                    .personName()

                s.isMdl -> MobileDrivingLicenceCredentialAdapter
                    .createFromStoreEntry(this, failingImageDecoder)
                    .personName()

                else -> null
            }
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    return listOfNotNull(schemeLabel, detail).joinToString(" - ")
}

private val failingImageDecoder: (ByteArray) -> Result<ImageBitmap> = {
    Result.failure(IllegalStateException("Image decoding is not needed for credential titles"))
}

private fun EuPidCredentialAdapter.personName() = joinName(givenName, familyName)

private fun MobileDrivingLicenceCredentialAdapter.personName() = joinName(givenName, familyName)

private fun joinName(givenName: String?, familyName: String?) = listOfNotNull(
    givenName?.takeIf { it.isNotBlank() },
    familyName?.takeIf { it.isNotBlank() },
).joinToString(" ")

package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.ageverification.AgeVerificationScheme
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.taxid.TaxIdScheme

fun SubjectCredentialStore.StoreEntry.displayTitle(schemeLabel: String): String {
    val detail = catchingUnwrapped {
        scheme.let { s ->
            when {
                s.isEuPid -> EuPidCredentialAdapter.createFromStoreEntry(this, failingImageDecoder)
                    .personName()

                s.isMdl -> MobileDrivingLicenceCredentialAdapter
                    .createFromStoreEntry(this, failingImageDecoder)
                    .personName()

                s is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialAdapter.createFromStoreEntry(this)
                    .personName()

                s is CompanyRegistrationScheme -> CompanyRegistrationCredentialAdapter.createFromStoreEntry(this)
                    .companyName

                s is HealthIdScheme -> HealthIdCredentialAdapter.createFromStoreEntry(this)
                    .healthInsuranceId

                s is TaxIdScheme -> TaxIdCredentialAdapter.createFromStoreEntry(this)
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

private fun CertificateOfResidenceCredentialAdapter.personName() = joinName(givenName, familyName)

private fun TaxIdCredentialAdapter.personName() = joinName(registeredGivenName, registeredFamilyName)

private fun joinName(givenName: String?, familyName: String?) = listOfNotNull(
    givenName?.takeIf { it.isNotBlank() },
    familyName?.takeIf { it.isNotBlank() },
).joinToString(" ")

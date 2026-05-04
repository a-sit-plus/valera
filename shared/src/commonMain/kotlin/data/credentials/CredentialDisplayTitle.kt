package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.ageverification.AgeVerificationScheme
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.taxid.TaxIdScheme

fun SubjectCredentialStore.StoreEntry.displayTitle(schemeLabel: String): String {
    val detail = runCatching {
        when (scheme) {
            is EuPidScheme,
            is EuPidSdJwtScheme -> EuPidCredentialAdapter.createFromStoreEntry(this, failingImageDecoder)
                .personName()

            is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialAdapter
                .createFromStoreEntry(this, failingImageDecoder)
                .personName()

            is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialAdapter.createFromStoreEntry(this)
                .personName()

            is CompanyRegistrationScheme -> CompanyRegistrationCredentialAdapter.createFromStoreEntry(this)
                .companyName

            is HealthIdScheme -> HealthIdCredentialAdapter.createFromStoreEntry(this)
                .healthInsuranceId

            is TaxIdScheme -> TaxIdCredentialAdapter.createFromStoreEntry(this)
                .personName()

            is AgeVerificationScheme -> null
            else -> null
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

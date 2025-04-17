package data.credentials

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme

fun SubjectCredentialStore.StoreEntry.toCredentialAdapter(
    decodeImage: (ByteArray) -> ImageBitmap?,
): CredentialAdapter? = when (scheme) {
    is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialAdapter.createFromStoreEntry(this)
    is CompanyRegistrationScheme -> CompanyRegistrationCredentialAdapter.createFromStoreEntry(this)
    is EuPidScheme -> EuPidCredentialAdapter.createFromStoreEntry(this, decodePortrait = decodeImage)
    is HealthIdScheme -> HealthIdCredentialAdapter.createFromStoreEntry(this)
    is IdAustriaScheme -> IdAustriaCredentialAdapter.createFromStoreEntry(this, decodeImage = decodeImage)
    is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(this, decodePortrait = decodeImage)
    is PowerOfRepresentationScheme -> PowerOfRepresentationCredentialAdapter.createFromStoreEntry(this)
    is TaxIdScheme -> TaxIdCredentialAdapter.createFromStoreEntry(this)
    null -> null
    else -> throw IllegalStateException("Unsupported credential scheme")
}
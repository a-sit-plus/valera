package data.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.ageverification.AgeVerificationScheme
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import data.Attribute

@Suppress("DEPRECATION")
@Composable
fun SubjectCredentialStore.StoreEntry.toCredentialAdapter(
    decodeImage: (ByteArray) -> Result<ImageBitmap>,
): CredentialAdapter? = when (scheme) {
    is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialAdapter.createFromStoreEntry(this)
    is CompanyRegistrationScheme -> CompanyRegistrationCredentialAdapter.createFromStoreEntry(this)
    is EuPidScheme -> EuPidCredentialAdapter.createFromStoreEntry(this, decodePortrait = decodeImage)
    is EuPidSdJwtScheme -> EuPidCredentialAdapter.createFromStoreEntry(this, decodePortrait = decodeImage)
    is HealthIdScheme -> HealthIdCredentialAdapter.createFromStoreEntry(this)
    is IdAustriaScheme -> IdAustriaCredentialAdapter.createFromStoreEntry(this, decodeImage = decodeImage)
    is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(this, decodePortrait = decodeImage)
    is AgeVerificationScheme -> AgeVerificationCredentialAdapter.createFromStoreEntry(this)
    is PowerOfRepresentationScheme -> PowerOfRepresentationCredentialAdapter.createFromStoreEntry(this)
    is TaxIdScheme -> TaxIdCredentialAdapter.createFromStoreEntry(this)
    else -> null
}


class FallbackCredentialAdapter(
    genericAttributeList: List<Pair<NormalizedJsonPath, Any>>,
    val credential: SubjectCredentialStore.StoreEntry
) : CredentialAdapter() {
    // trying our best to map the values to attributes
    private val mapping = genericAttributeList.toMap()

    override fun getAttribute(path: NormalizedJsonPath): Attribute? = mapping[path]
        ?.let { Attribute.fromValue(it) }

    override val representation = credential.representation
    override val scheme = credential.scheme!!
}

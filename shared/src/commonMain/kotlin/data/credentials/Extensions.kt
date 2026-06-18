package data.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.ageverification.AgeVerificationScheme
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import data.Attribute

@Suppress("DEPRECATION")
@Composable
fun SubjectCredentialStore.StoreEntry.toCredentialAdapter(
    decodeImage: (ByteArray) -> Result<ImageBitmap>,
): CredentialAdapter? = scheme.let { s ->
    when {
        s.isEuPid -> EuPidCredentialAdapter.createFromStoreEntry(this, decodePortrait = decodeImage)
        s.isMdl -> MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(this, decodePortrait = decodeImage)
        s is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialAdapter.createFromStoreEntry(this)
        s is CompanyRegistrationScheme -> CompanyRegistrationCredentialAdapter.createFromStoreEntry(this)
        s is EhicScheme -> EhicCredentialAdapter.createFromStoreEntry(this)
        s is HealthIdScheme -> HealthIdCredentialAdapter.createFromStoreEntry(this)
        s is AgeVerificationScheme -> AgeVerificationCredentialAdapter.createFromStoreEntry(this)
        s is PowerOfRepresentationScheme -> PowerOfRepresentationCredentialAdapter.createFromStoreEntry(this)
        s is TaxIdScheme -> TaxIdCredentialAdapter.createFromStoreEntry(this)
        else -> null
    }
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

package at.asitplus.wallet.app.common

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eprescription.EPrescriptionScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import data.credentials.CredentialAdapter
import data.credentials.CredentialAdapter.Companion.toAttributeMap
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import data.credentials.CredentialAttributeTranslator
import data.credentials.EuPidCredentialAdapter
import data.credentials.IdAustriaCredentialAdapter
import data.credentials.MobileDrivingLicenceCredentialAdapter
import data.dcapi.CredentialField
import data.dcapi.CredentialsContainer
import data.dcapi.DisplayInfoField
import data.dcapi.IdentityCredentialEntry
import data.dcapi.IdentityCredentialField
import data.storage.StoreContainer
import io.github.aakira.napier.Napier

class DCAPIService(val platformAdapter: PlatformAdapter) {

    fun registerCredentialWithSystem(container: StoreContainer) {
        Napier.d("Preparing registration of credentials with the system")

        val identityCredentialsEntries: List<IdentityCredentialEntry> = container.credentials.map { (_, storeEntry) ->
            val imageDecoder: (ByteArray) -> ImageBitmap = { byteArray -> platformAdapter.decodeImage(byteArray)}
            var picture : ByteArray? = null
            val attributeTranslator = CredentialAttributeTranslator[storeEntry.scheme]
            val friendlyName = when (storeEntry.scheme) {
                is IdAustriaScheme -> {
                    val credential = IdAustriaCredentialAdapter.createFromStoreEntry(storeEntry, imageDecoder)
                    picture = credential.portraitRaw
                    "ID Austria"
                }
                is EuPidScheme -> {
                    EuPidCredentialAdapter.createFromStoreEntry(storeEntry)
                    "EU PID"
                }
                is MobileDrivingLicenceScheme -> {
                    val credential = MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(storeEntry, imageDecoder)
                    picture = credential.portraitRaw
                    "Führerschein"
                }
                is PowerOfRepresentationScheme -> {
                    "Power of Representation"
                }
                is CertificateOfResidenceScheme -> {
                    "Certifiate of Residence"
                }
                is EPrescriptionScheme -> {
                    "E-Prescription"
                }
                else -> {
                    ""
                }
            }

            val entries: List<IdentityCredentialField> = when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    TODO("Operation not yet supported")
                }
                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    val attributeMap = storeEntry.toAttributeMap()
                    IdentityCredentialField.fromAttributeMap(attributeMap, attributeTranslator)
                }
                is SubjectCredentialStore.StoreEntry.Iso -> {
                    val namespaceAttributeMap = storeEntry.toNamespaceAttributeMap()
                    IdentityCredentialField.fromNamespaceAttributeMap(namespaceAttributeMap, attributeTranslator)
                }
            }

            //TODO can we get a better ID?
            val id = CredentialAdapter.getId(storeEntry).hashCode()
            IdentityCredentialEntry(
                id,
                CredentialField(
                    storeEntry.scheme?.isoDocType ?: "",
                    DisplayInfoField(friendlyName, "Valera", null, null),
                    entries
                ),
                picture
            )
        }
        platformAdapter.registerWithDigitalCredentialsAPI(CredentialsContainer(identityCredentialsEntries))
        Napier.d("Registered credentials with the system")
    }
}
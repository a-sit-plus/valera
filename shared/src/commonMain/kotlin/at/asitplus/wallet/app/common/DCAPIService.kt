package at.asitplus.wallet.app.common

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.app_display_name
import at.asitplus.wallet.app.common.dcapi.CredentialField
import at.asitplus.wallet.app.common.dcapi.CredentialsContainer
import at.asitplus.wallet.app.common.dcapi.DisplayInfoField
import at.asitplus.wallet.app.common.dcapi.IdentityCredentialEntry
import at.asitplus.wallet.app.common.dcapi.IdentityCredentialField
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabelNonCompose
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.credentials.CredentialAdapter
import data.credentials.CredentialAdapter.Companion.toAttributeMap
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import data.credentials.CredentialAttributeTranslator
import data.credentials.IdAustriaCredentialAdapter
import data.credentials.MobileDrivingLicenceCredentialAdapter
import data.storage.StoreContainer
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.getString

class DCAPIService(val platformAdapter: PlatformAdapter) {

    suspend fun registerCredentialWithSystem(container: StoreContainer) {
        Napier.d("Preparing registration of credentials with the system")

        val identityCredentialsEntries: List<IdentityCredentialEntry> = container.credentials.map { (_, storeEntry) ->
            val imageDecoder: (ByteArray) -> ImageBitmap = { byteArray -> platformAdapter.decodeImage(byteArray) }
            val attributeTranslator = CredentialAttributeTranslator[storeEntry.scheme]
                ?: return
            val friendlyName = storeEntry.scheme.uiLabelNonCompose()
            val picture: ByteArray? = when (storeEntry.scheme) {
                is IdAustriaScheme ->
                    IdAustriaCredentialAdapter.createFromStoreEntry(storeEntry, imageDecoder).portraitRaw

                is MobileDrivingLicenceScheme ->
                    MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(storeEntry, imageDecoder).portraitRaw

                else -> null
            }

            val entries: List<IdentityCredentialField> = when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    TODO("Operation not yet supported")
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    IdentityCredentialField.fromAttributeMap(storeEntry.toAttributeMap(), attributeTranslator)
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    storeEntry.toNamespaceAttributeMap()
                        ?.let { IdentityCredentialField.fromNamespaceAttributeMap(it, attributeTranslator) } ?: listOf()
                }
            }

            //TODO can we get a better ID?
            val id = CredentialAdapter.getId(storeEntry).hashCode()
            IdentityCredentialEntry(
                id,
                CredentialField(
                    storeEntry.scheme?.isoDocType ?: "",
                    DisplayInfoField(friendlyName, getString(Res.string.app_display_name), null, null),
                    entries
                ),
                picture
            )
        }
        platformAdapter.registerWithDigitalCredentialsAPI(CredentialsContainer(identityCredentialsEntries))
        Napier.d("Registered credentials with the system")
    }
}
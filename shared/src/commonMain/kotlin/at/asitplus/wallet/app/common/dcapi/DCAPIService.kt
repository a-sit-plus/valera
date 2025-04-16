package at.asitplus.wallet.app.common.dcapi

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.app_display_name
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.dcapi.data.CredentialEntry
import at.asitplus.wallet.app.common.dcapi.data.CredentialList
import at.asitplus.wallet.app.common.dcapi.data.IsoCredentialNamespaces
import at.asitplus.wallet.app.common.dcapi.data.IsoEntry
import at.asitplus.wallet.app.common.dcapi.data.SdJwtEntry
import at.asitplus.wallet.app.common.decodeImage
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
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.getString

class DCAPIService(val platformAdapter: PlatformAdapter) {
    private val imageDecoder: (ByteArray) -> ImageBitmap = { byteArray -> platformAdapter.decodeImage(byteArray) }

    suspend fun registerCredentialWithSystem(container: StoreContainer, scope: CoroutineScope) {
        Napier.d("DC API: Preparing registration of updated credentials with the system")

        val credentialListEntries = container.credentials.map { (_, storeEntry) ->
            val attributeTranslator = CredentialAttributeTranslator[storeEntry.scheme] ?: return
            val friendlyName = storeEntry.scheme.uiLabelNonCompose()
            //TODO can we get a better ID?
            val id = CredentialAdapter.getId(storeEntry).hashCode().toString()
            val format = storeEntry.scheme?.isoDocType ?: ""
            val appName = getString(Res.string.app_display_name)
            val picture: ByteArray? = when (storeEntry.scheme) {
                is IdAustriaScheme ->
                    IdAustriaCredentialAdapter.createFromStoreEntry(storeEntry, imageDecoder).portraitRaw
                is MobileDrivingLicenceScheme ->
                    MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(storeEntry, imageDecoder).portraitRaw
                else -> null
            }

            val sdJwtEntry: SdJwtEntry?
            val isoEntry: IsoEntry?
             when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    //IdentityCredentialField.fromAttributeMap(storeEntry.toAttributeMap(), attributeTranslator)
                    storeEntry.toAttributeMap()
                    sdJwtEntry = SdJwtEntry("TODO()")
                    isoEntry = null
                }
                 is SubjectCredentialStore.StoreEntry.Iso -> {
                     val isoCredentialNamespaces = storeEntry.toNamespaceAttributeMap() ?.let {
                         IsoCredentialNamespaces.fromNamespaceAttributeMap(it, attributeTranslator)
                     }
                     isoEntry = IsoEntry(id, format, isoCredentialNamespaces!!)
                     sdJwtEntry = null
                 }
                is SubjectCredentialStore.StoreEntry.Vc -> TODO("Operation not yet supported")
            }
            val credentialEntry = CredentialEntry(appName, friendlyName, bitmap = picture, isoEntry = isoEntry, sdJwtEntry = sdJwtEntry)
             credentialEntry
        }

        val credentialList = CredentialList(credentialListEntries)
        platformAdapter.registerWithDigitalCredentialsAPI(credentialList, scope)
        Napier.d("DC API: Registered ${credentialList.entries.size} credentials with the system")
    }
}
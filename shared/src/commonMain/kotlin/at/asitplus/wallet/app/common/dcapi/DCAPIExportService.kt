package at.asitplus.wallet.app.common.dcapi

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catching
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.app_display_name
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialEntry
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialList
import at.asitplus.wallet.app.common.dcapi.data.export.IsoEntry
import at.asitplus.wallet.app.common.dcapi.data.export.SdJwtEntry
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabelNonCompose
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.credentials.CredentialAdapter.Companion.toAttributeMap
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import data.credentials.CredentialAttributeTranslator
import data.credentials.EuPidCredentialAdapter
import data.credentials.IdAustriaCredentialAdapter
import data.credentials.MobileDrivingLicenceCredentialAdapter
import data.storage.StoreContainer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.getString

class DCAPIExportService(private val platformAdapter: PlatformAdapter) {
    private val imageDecoder: (ByteArray) -> Result<ImageBitmap> = { platformAdapter.decodeImage(it) }

    suspend fun registerCredentialWithSystem(container: StoreContainer, scope: CoroutineScope) {
        Napier.d("DC API: Preparing registration of updated credentials with the system")

        val credentialListEntries = container.credentials.mapNotNull { (_, storeEntry) ->
            catching { storeEntry.toCredentialEntry() }.getOrNull()
        }

        val credentialList = CredentialList(credentialListEntries)
        platformAdapter.registerWithDigitalCredentialsAPI(credentialList, scope)
        Napier.d("DC API: Registering ${credentialList.entries.size} credentials with the system")
    }

    private suspend fun SubjectCredentialStore.StoreEntry.toCredentialEntry() = when (this) {
        is SubjectCredentialStore.StoreEntry.SdJwt -> CredentialEntry(
            title = scheme.uiLabelNonCompose(),
            subtitle = getString(Res.string.app_display_name),
            bitmap = extractPicture(),
            sdJwtEntry = toSdJwtEntry()
        )

        is SubjectCredentialStore.StoreEntry.Iso -> CredentialEntry(
            title = scheme.uiLabelNonCompose(),
            subtitle = getString(Res.string.app_display_name),
            bitmap = extractPicture(),
            isoEntry = toIsoEntry()
        )

        is SubjectCredentialStore.StoreEntry.Vc -> null
    }

    private suspend fun SubjectCredentialStore.StoreEntry.Iso.toIsoEntry() = IsoEntry(
        id = getDcApiId(),
        docType = scheme?.isoDocType ?: "",
        isoNamespaces = toNamespaceAttributeMap()?.let {
            IsoEntry.isoNamespacesFromNamespaceAttributeMap(it, getTranslator())
        } ?: mapOf())

    private suspend fun SubjectCredentialStore.StoreEntry.SdJwt.toSdJwtEntry() = SdJwtEntry(
        jwtId = getDcApiId(),
        verifiableCredentialType = sdJwt.verifiableCredentialType,
        claims = SdJwtEntry.fromAttributeMap(toAttributeMap(), getTranslator())
    )

    private fun SubjectCredentialStore.StoreEntry.getTranslator() = CredentialAttributeTranslator[scheme]
        ?: throw IllegalStateException("Attribute translator not implemented")

    private fun SubjectCredentialStore.StoreEntry.extractPicture() = when (scheme) {
        is IdAustriaScheme ->
            IdAustriaCredentialAdapter.createFromStoreEntry(this, imageDecoder).portraitRaw

        is MobileDrivingLicenceScheme ->
            MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(this, imageDecoder).portraitRaw

        is EuPidSdJwtScheme,
        is EuPidScheme ->
            EuPidCredentialAdapter.createFromStoreEntry(this, imageDecoder).portraitRaw

        else -> null
    }
}
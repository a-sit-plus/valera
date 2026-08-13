package at.asitplus.wallet.app.common.dcapi

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catchingUnwrapped
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.app_display_name
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialEntry
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import at.asitplus.wallet.app.common.dcapi.data.export.IsoMdocEntry
import at.asitplus.wallet.app.common.dcapi.data.export.SdJwtEntry
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabelNonCompose
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.lib.data.SdJwtFallbackCredentialScheme
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import data.credentials.EuPidCredentialAdapter
import data.credentials.MobileDrivingLicenceCredentialAdapter
import data.credentials.metadataLabel
import data.credentials.toGenericAttributeList
import data.storage.StoreContainer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.getString

class DCAPIExportService(private val platformAdapter: PlatformAdapter) {
    private val imageDecoder: (ByteArray) -> Result<ImageBitmap> = { platformAdapter.decodeImage(it) }

    suspend fun registerCredentialWithSystem(container: StoreContainer, scope: CoroutineScope) {
        Napier.d("DC API: Preparing registration of updated credentials with the system")

        val credentialListEntries = container.credentials.mapNotNull { (_, storeEntry) ->
            catchingUnwrapped { storeEntry.toCredentialEntry() }
                .onFailure { Napier.w("Failed to convert credential to DC API entry", it) }
                .getOrNull()
        }

        val credentialRegistry = CredentialRegistry.create(credentialListEntries)
        platformAdapter.registerWithDigitalCredentialsAPI(credentialRegistry, scope)
        Napier.d("DC API: Registering ${credentialRegistry.credentials.size} credentials with the system")
    }

    private suspend fun SubjectCredentialStore.StoreEntry.toCredentialEntry(): CredentialEntry? {
        if (this is SubjectCredentialStore.StoreEntry.Vc) return null

        val scheme = catchingUnwrapped { resolveScheme() }
            .onFailure {
                Napier.w("Failed to resolve credential metadata for DC API registration; using fallback", it)
            }
            .getOrElse { fallbackScheme() }

        return when (this) {
            is SubjectCredentialStore.StoreEntry.SdJwt -> CredentialEntry(
                title = scheme.uiLabelNonCompose(),
                subtitle = getString(Res.string.app_display_name),
                bitmap = extractPicture(scheme),
                sdJwtEntry = toSdJwtEntry(scheme),
            )

            is SubjectCredentialStore.StoreEntry.Iso -> CredentialEntry(
                title = scheme.uiLabelNonCompose(),
                subtitle = getString(Res.string.app_display_name),
                bitmap = extractPicture(scheme),
                isoEntry = toIsoEntry(scheme),
            )

            is SubjectCredentialStore.StoreEntry.Vc -> null
        }
    }

    private suspend fun SubjectCredentialStore.StoreEntry.Iso.toIsoEntry(
        scheme: CredentialScheme,
    ): IsoMdocEntry = IsoMdocEntry(
        id = getDcApiId(),
        docType = schemeIdentifier ?: scheme.isoDocType ?: "unknown",
        isoNamespaces = toNamespaceAttributeMap()?.let {
            IsoMdocEntry.isoNamespacesFromNamespaceAttributeMap(it) { path -> scheme.metadataLabel(path) }
        } ?: mapOf()
    )

    private suspend fun SubjectCredentialStore.StoreEntry.SdJwt.toSdJwtEntry(
        scheme: CredentialScheme,
    ): SdJwtEntry = SdJwtEntry(
        jwtId = getDcApiId(),
        verifiableCredentialType = schemeIdentifier ?: scheme.sdJwtType ?: "unknown",
        claims = SdJwtEntry.fromAttributeList(toGenericAttributeList()) { path -> scheme.metadataLabel(path) }
    )

    private fun SubjectCredentialStore.StoreEntry.extractPicture(scheme: CredentialScheme) = scheme.let { s ->
        when {
            s.isMdl -> MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(this, s, imageDecoder).portraitRaw
            s.isEuPid -> EuPidCredentialAdapter.createFromStoreEntry(this, s, imageDecoder).portraitRaw

            else -> null
        }
    }

    private fun SubjectCredentialStore.StoreEntry.fallbackScheme(): CredentialScheme = when (this) {
        is SubjectCredentialStore.StoreEntry.SdJwt -> SdJwtFallbackCredentialScheme(schemeIdentifier ?: sdJwt.verifiableCredentialType)
        is SubjectCredentialStore.StoreEntry.Iso -> IsoMdocFallbackCredentialScheme(schemeIdentifier ?: issuerSigned.issuerAuth.payload?.docType ?: "unknown")
        is SubjectCredentialStore.StoreEntry.Vc -> error("JWT VC credentials are not registered with the DC API")
    }
}

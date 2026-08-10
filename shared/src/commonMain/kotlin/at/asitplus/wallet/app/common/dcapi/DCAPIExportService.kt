package at.asitplus.wallet.app.common.dcapi

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catching
import at.asitplus.catchingUnwrapped
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.app_display_name
import at.asitplus.valera.resources.dcapi_issuing_credential_title
import at.asitplus.wallet.app.common.AV_DOC_TYPE
import at.asitplus.wallet.app.common.COMPANY_REGISTRATION_VCT
import at.asitplus.wallet.app.common.COR_VCT
import at.asitplus.wallet.app.common.EHIC_VCT
import at.asitplus.wallet.app.common.HEALTH_ID_VCT
import at.asitplus.wallet.app.common.POR_VCT
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.TAX_ID_VCT
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialEntry
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import at.asitplus.wallet.app.common.dcapi.data.export.IsoMdocEntry
import at.asitplus.wallet.app.common.dcapi.data.export.IssuingCredentialEntry
import at.asitplus.wallet.app.common.dcapi.data.export.SdJwtEntry
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabelNonCompose
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.lib.data.SdJwtFallbackCredentialScheme
import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.eupidsdjwt.EU_PID_SD_JWT_VCT
import at.asitplus.wallet.mdl.MDL_DOCTYPE
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

        val credentialRegistry = CredentialRegistry.create(
            credentials = credentialListEntries,
            issuingCredential = createIssuingCredentialEntry(),
        )
        platformAdapter.registerWithDigitalCredentialsAPI(credentialRegistry, scope)
        Napier.d("DC API: Registering ${credentialRegistry.credentials.size} credentials with the system")
    }

    private suspend fun createIssuingCredentialEntry() = IssuingCredentialEntry(
        title = getString(Res.string.dcapi_issuing_credential_title),
        subtitle = getString(Res.string.app_display_name),
        id = ISSUING_CREDENTIAL_ID,
        mdocDocTypes = supportedIssuingMdocDocTypes,
        sdJwtVcts = supportedIssuingSdJwtVcts,
    )

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
        docType = schemeIdentifier,
        isoNamespaces = toNamespaceAttributeMap()?.let {
            IsoMdocEntry.isoNamespacesFromNamespaceAttributeMap(it) { path -> scheme.metadataLabel(path) }
        } ?: mapOf()
    )

    private suspend fun SubjectCredentialStore.StoreEntry.SdJwt.toSdJwtEntry(
        scheme: CredentialScheme,
    ): SdJwtEntry = SdJwtEntry(
        jwtId = getDcApiId(),
        verifiableCredentialType = schemeIdentifier,
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
        is SubjectCredentialStore.StoreEntry.SdJwt -> SdJwtFallbackCredentialScheme(schemeIdentifier)
        is SubjectCredentialStore.StoreEntry.Iso -> IsoMdocFallbackCredentialScheme(schemeIdentifier)
        is SubjectCredentialStore.StoreEntry.Vc -> error("JWT VC credentials are not registered with the DC API")
    }

    companion object {
        internal const val ISSUING_CREDENTIAL_ID = "dcapi-issuing-credential"

        internal val supportedIssuingMdocDocTypes = listOf(
            EU_PID_DOCTYPE,
            MDL_DOCTYPE,
            AV_DOC_TYPE,
        )

        internal val supportedIssuingSdJwtVcts = listOf(
            "EuPid2023",
            EU_PID_SD_JWT_VCT,
            MDL_DOCTYPE,
            EHIC_VCT,
            TAX_ID_VCT,
            COR_VCT,
            COMPANY_REGISTRATION_VCT,
            POR_VCT,
            HEALTH_ID_VCT,
            AV_DOC_TYPE,
            "urn:eidgvat:eid.status.full"
        )
    }
}

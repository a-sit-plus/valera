package at.asitplus.wallet.app.common.dcapi

import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.wallet.app.common.AV_DOC_TYPE
import at.asitplus.wallet.app.common.COMPANY_REGISTRATION_VCT
import at.asitplus.wallet.app.common.COR_VCT
import at.asitplus.wallet.app.common.EHIC_VCT
import at.asitplus.wallet.app.common.HEALTH_ID_VCT
import at.asitplus.wallet.app.common.POR_VCT
import at.asitplus.wallet.app.common.TAX_ID_VCT
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import at.asitplus.wallet.app.common.dcapi.data.export.IssuingCredentialEntry
import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.eupidsdjwt.EU_PID_SD_JWT_VCT
import at.asitplus.wallet.mdl.MDL_DOCTYPE
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

class DCAPIExportServiceTest {
    @Test
    fun supportedIssuingTypesContainAllKnownCredentialTypes() {
        assertEquals(
            listOf(EU_PID_DOCTYPE, MDL_DOCTYPE, AV_DOC_TYPE),
            DCAPIExportService.supportedIssuingMdocDocTypes,
        )
        assertEquals(
            listOf(
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
                "urn:eidgvat:eid.status.full",
            ),
            DCAPIExportService.supportedIssuingSdJwtVcts,
        )
    }

    @Test
    fun issuingCredentialSurvivesRegistrySerialization() {
        val issuingCredential = IssuingCredentialEntry(
            title = "Issuing Credential",
            subtitle = "Valera",
            id = DCAPIExportService.ISSUING_CREDENTIAL_ID,
            mdocDocTypes = DCAPIExportService.supportedIssuingMdocDocTypes,
            sdJwtVcts = DCAPIExportService.supportedIssuingSdJwtVcts,
        )
        val registry = CredentialRegistry.create(
            credentials = emptyList(),
            issuingCredential = issuingCredential,
        )

        val encoded = coseCompliantSerializer.encodeToByteArray(registry)
        val decoded = coseCompliantSerializer.decodeFromByteArray<CredentialRegistry>(encoded)

        assertEquals(emptyList(), decoded.credentials)
        assertEquals(issuingCredential, decoded.issuingCredential)
    }
}

package at.asitplus.wallet.app.common.validation

import at.asitplus.catchingUnwrapped
import at.asitplus.etsi.relyingParty.WrpIdentifier
import at.asitplus.etsi.relyingParty.WrpMultiLangString
import at.asitplus.etsi.relyingParty.WrpRegistrarDataset
import at.asitplus.openid.IssuerMetadata
import at.asitplus.openid.OpenIdConstants
import at.asitplus.openid.SupportedCredentialFormat
import at.asitplus.openid.SupportedCredentialFormatIsoMdoc
import at.asitplus.openid.SupportedCredentialFormatSdJwt
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.lib.oidvci.LoadedIssuerMetadata
import kotlinx.serialization.Serializable

/**
 * The validation of a credential issuer from its metadata, as shown to the user before issuance: the report of its
 * signed metadata, and its registration as stated in the metadata.
 */
@Serializable
data class IssuerValidationUi(
    /** The report of the signed metadata, `null` for unsigned ones, whose issuer can not be verified. */
    val outcome: ValidationOutcomeUi?,
    /** The registration information of the issuer in `issuer_info`, verified only if [verified]. */
    val registration: IssuerRegistrationUi?,
) {
    /** Whether the metadata are signed, see [outcome]. */
    val signed: Boolean
        get() = outcome != null

    /** Whether the signed metadata have been accepted, i.e. the issuer and its registration are trusted. */
    val verified: Boolean
        get() = outcome?.accepted == true

    /**
     * Whether the issuer is registered for the credential type of [format], `null` if that can not be told, e.g.
     * without registration information.
     */
    fun registeredFor(format: SupportedCredentialFormat): Boolean? {
        val attestations = registration?.attestations?.takeIf { it.isNotEmpty() } ?: return null
        val type = when (format) {
            is SupportedCredentialFormatIsoMdoc -> format.docType
            is SupportedCredentialFormatSdJwt -> format.sdJwtVcType
            else -> return null
        }
        return type in attestations
    }

    companion object {
        fun of(loaded: LoadedIssuerMetadata) = when (loaded) {
            is LoadedIssuerMetadata.Signed -> IssuerValidationUi(
                outcome = loaded.validation.report.toUi(),
                registration = (loaded.metadata ?: loaded.untrustedMetadata)?.registration(),
            )

            is LoadedIssuerMetadata.Unsigned -> unsigned(loaded.metadata)
        }

        /** Metadata that are not signed, e.g. received with an offer through the Digital Credentials API. */
        fun unsigned(metadata: IssuerMetadata) = IssuerValidationUi(
            outcome = null,
            registration = metadata.registration(),
        )
    }
}

/** What a registrar says about a credential issuer (ETSI TS 119 472-3 V1.1.1, 4.2.3). */
@Serializable
data class IssuerRegistrationUi(
    val identifiers: List<WrpIdentifier>,
    val description: List<WrpMultiLangString>,
    val registryUri: String,
    /** The `doctype` or `vct` values of the credentials the issuer is registered to provide. */
    val attestations: List<String>,
)

private fun IssuerMetadata.registration(): IssuerRegistrationUi? = catchingUnwrapped {
    val data = issuerInfo?.firstOrNull { it.format == OpenIdConstants.IssuerInfo.REGISTRAR_DATASET_FORMAT }?.data
        ?: return@catchingUnwrapped null
    val dataset = joseCompliantSerializer.decodeFromJsonElement(WrpRegistrarDataset.serializer(), data)
    IssuerRegistrationUi(
        identifiers = dataset.identifier,
        description = dataset.srvDescription,
        registryUri = dataset.registryURI,
        attestations = dataset.providesAttestations.flatMap {
            listOfNotNull(it.meta.doctypeValue) + it.meta.vctValues.orEmpty()
        },
    )
}.getOrNull()

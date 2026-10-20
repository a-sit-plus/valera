package at.asitplus.wallet.app.common.validation

import at.asitplus.etsi.relyingParty.WrpCredential
import at.asitplus.etsi.relyingParty.WrpLangString
import at.asitplus.etsi.relyingParty.WrpPayload
import at.asitplus.openid.SupportedCredentialFormat
import at.asitplus.openid.SupportedCredentialFormatIsoMdoc
import at.asitplus.openid.SupportedCredentialFormatSdJwt
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
                registration = loaded.validation.issuer?.registrationCertificate?.toRegistrationUi()
                    ?: loaded.validation.untrustedRegistration?.toRegistrationUi(),
            )

            is LoadedIssuerMetadata.Unsigned -> unsigned()
        }

        /** Metadata that are not signed, e.g. received with an offer through the Digital Credentials API. */
        fun unsigned() = IssuerValidationUi(
            outcome = null,
            registration = null,
        )
    }
}

/**
 * What a registrar says about a credential issuer: its registration information (ETSI TS 119 472-3 V1.1.1, 4.2.3),
 * or what its registration certificate (WRPRC) says, if the metadata carry no registration information.
 */
@Serializable
data class IssuerRegistrationUi(
    /** The name of the issuer, which only a WRPRC carries. */
    val name: String? = null,
    /** The identifiers of the issuer, e.g. `LEI: 1234`. */
    val identifiers: List<String>,
    val description: List<WrpLangString>,
    val registryUri: String,
    /** The `doctype` or `vct` values of the credentials the issuer is registered to provide. */
    val attestations: List<String>,
)

private fun WrpPayload.toRegistrationUi() = IssuerRegistrationUi(
    name = name ?: subjectLegalName,
    identifiers = listOf(subjectIdentifier),
    description = srvDescription.flatten().map { it },
    registryUri = registryUri,
    attestations = providesAttestations.orEmpty().attestationTypes(),
)

private fun List<WrpCredential>.attestationTypes() =
    flatMap { listOfNotNull(it.meta.doctypeValue) + it.meta.vctValues.orEmpty() }

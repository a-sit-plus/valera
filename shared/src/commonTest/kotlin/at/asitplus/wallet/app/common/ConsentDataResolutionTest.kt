package at.asitplus.wallet.app.common

import at.asitplus.dif.Constraint
import at.asitplus.dif.ConstraintField
import at.asitplus.dif.DifInputDescriptor
import at.asitplus.dif.FormatContainerJwt
import at.asitplus.dif.FormatHolder
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment.NameSegment
import at.asitplus.openid.dcql.DCQLClaimsPathPointer
import at.asitplus.openid.dcql.DCQLClaimsQueryList
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLJsonClaimsQuery
import at.asitplus.openid.dcql.DCQLSdJwtCredentialMetadataAndValidityConstraints
import at.asitplus.openid.dcql.DCQLSdJwtCredentialQuery
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.lib.LibraryInitializer
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.ISO_MDOC
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.SD_JWT
import at.asitplus.wallet.lib.data.CredentialMetadataRegistry
import at.asitplus.wallet.lib.data.ResolvedCredentialMetadata
import at.asitplus.wallet.sdjwt.SdJwtTypeMetadataDefinition
import at.asitplus.wallet.sdjwt.SdJwtTypeMetadataDocument
import at.asitplus.wallet.sdjwt.SdJwtTypeMetadataVckExtensions
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import at.asitplus.openid.CredentialFormatEnum as OpenIdCredentialFormatEnum
import at.asitplus.wallet.sdjwt.CredentialFormatEnum as SdJwtCredentialFormatEnum

/**
 * The consent screen must resolve claim labels from type metadata even in a freshly started
 * process whose in-memory scheme index is still cold, e.g. the iOS identity provider extension
 * answering a DC API request: scheme resolution has to go through the (suspending) credential
 * metadata registry instead of only looking at already-registered schemes.
 */
class ConsentDataResolutionTest {

    /** Input descriptors like those built from the iOS DC API mdoc pre-request summary. */
    @Test
    fun resolvesMdocClaimLabelsThroughMetadataRegistry() = runTest {
        registerTestMetadata()
        val descriptor = DifInputDescriptor(
            id = TEST_DOCTYPE,
            format = FormatHolder(msoMdoc = FormatContainerJwt()),
            constraints = Constraint(
                fields = setOf(
                    ConstraintField(
                        path = listOf(
                            NormalizedJsonPath(
                                NameSegment(TEST_NAMESPACE),
                                NameSegment("family_name"),
                            ).toString()
                        ),
                    )
                )
            ),
        )

        val (representation, scheme, attributes) = descriptor.extractConsentData()

        assertEquals(ISO_MDOC, representation)
        assertEquals(TEST_LABEL, scheme.getLocalization(attributes.keys.single()))
    }

    /** A DCQL query like an Android DC API / OpenID4VP request for an SD-JWT credential. */
    @Test
    fun resolvesSdJwtClaimLabelsThroughMetadataRegistry() = runTest {
        registerTestMetadata()
        val query = DCQLSdJwtCredentialQuery(
            id = DCQLCredentialQueryIdentifier("query"),
            format = OpenIdCredentialFormatEnum.DC_SD_JWT,
            meta = DCQLSdJwtCredentialMetadataAndValidityConstraints(vctValues = listOf(TEST_VCT)),
            claims = DCQLClaimsQueryList(DCQLJsonClaimsQuery(path = DCQLClaimsPathPointer("family_name"))),
        )

        val (representation, scheme, claimReferences) = query.extractConsentData()

        assertEquals(SD_JWT, representation)
        val reference = assertNotNull(assertNotNull(claimReferences).filterNotNull().single())
        assertEquals(TEST_LABEL, scheme.getLocalization(reference))
    }

    companion object {
        private const val TEST_VCT = "urn:example:valera:consent-test"
        private const val TEST_DOCTYPE = "org.example.valera.consent-test"
        private const val TEST_NAMESPACE = "org.example.valera.consent-test.ns"
        private const val TEST_LABEL = "Family name test"

        private var registered = false

        private fun registerTestMetadata() {
            if (registered) return
            registered = true
            LibraryInitializer.registerCredentialMetadataRegistry(
                TestMetadataRegistry(
                    mapOf(
                        (TEST_DOCTYPE to ISO_MDOC) to resolvedMetadata(
                            vct = TEST_DOCTYPE,
                            claimPath = listOf(TEST_NAMESPACE, "family_name"),
                            vckExtensions = SdJwtTypeMetadataVckExtensions(
                                format = SdJwtCredentialFormatEnum.MSO_MDOC,
                                isoDocType = TEST_DOCTYPE,
                                isoNamespace = TEST_NAMESPACE,
                            ),
                        ),
                        (TEST_VCT to SD_JWT) to resolvedMetadata(
                            vct = TEST_VCT,
                            claimPath = listOf("family_name"),
                        ),
                    )
                )
            )
        }

        private fun resolvedMetadata(
            vct: String,
            claimPath: List<String>,
            vckExtensions: SdJwtTypeMetadataVckExtensions? = null,
        ): ResolvedCredentialMetadata {
            val decoded = joseCompliantSerializer.decodeFromString(
                SdJwtTypeMetadataDocument.serializer(),
                """
                {
                  "vct": "$vct",
                  "name": "Consent test credential",
                  "claims": [
                    {
                      "path": [${claimPath.joinToString(",") { "\"$it\"" }}],
                      "display": [{ "locale": "en", "label": "$TEST_LABEL" }]
                    }
                  ]
                }
                """.trimIndent()
            ).definition
            return ResolvedCredentialMetadata(
                metadata = SdJwtTypeMetadataDefinition(
                    vct = decoded.vct,
                    claims = decoded.claims,
                    vckExtensions = vckExtensions,
                ).toSdJwtTypeMetadata(),
                loadedFrom = "https://metadata.example.test/consent-test.json",
            )
        }
    }

    /** Resolves on demand only, like a remote registry: nothing is pre-seeded into the synchronous scheme index. */
    private class TestMetadataRegistry(
        private val entries: Map<Pair<String, CredentialRepresentation>, ResolvedCredentialMetadata>,
    ) : CredentialMetadataRegistry {
        override suspend fun findEntry(identifier: String, representation: CredentialRepresentation) =
            entries[identifier to representation]
    }
}

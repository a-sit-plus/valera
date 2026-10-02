package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.KmmResult
import at.asitplus.data.NonEmptyList.Companion.nonEmptyListOf
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLCredentialQueryList
import at.asitplus.openid.dcql.DCQLQuery
import at.asitplus.openid.dcql.DCQLSdJwtCredentialMetadataAndValidityConstraints
import at.asitplus.openid.dcql.DCQLSdJwtCredentialQuery
import at.asitplus.wallet.app.common.relyingParty.ui.WrpValidationError
import at.asitplus.wallet.app.common.relyingParty.ui.toRequestValidationData
import at.asitplus.wallet.app.common.relyingParty.ui.toWrprcRequestValidationData
import at.asitplus.wallet.app.common.relyingParty.ui.toWrprcRequestValidationGroups
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.RequestDataValidity
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpCredentialRequest
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpRegistrationCertificateValidation
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrprcValidationResult
import at.asitplus.wallet.lib.data.MdocClaimReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MultiSignedWrpValidationTest {

    private val query = WrpCredentialRequest.WrpDcqlCredentialQuery(
        DCQLSdJwtCredentialQuery(
            id = DCQLCredentialQueryIdentifier("pid"),
            meta = DCQLSdJwtCredentialMetadataAndValidityConstraints(vctValues = listOf("urn:eudi:pid:1")),
        )
    )
    private val givenName = MdocClaimReference("eu.europa.ec.eudi.pid.1", "given_name")
    private val familyName = MdocClaimReference("eu.europa.ec.eudi.pid.1", "family_name")

    private fun identity(
        wrpIdentifier: String? = "WRP-1",
        validWrprc: Boolean? = true,
        allowed: List<Pair<MdocClaimReference, Boolean>> = listOf(givenName to true),
    ) = WrpValidationResult(
        accessCertificate = wrpIdentifier?.let { WrpacValidation(validLinkage = true, wrpIdentifier = it) },
        accessCertificateError = if (wrpIdentifier == null) "not a WRPAC" else null,
        registrationCertificate = validWrprc?.let {
            WrprcValidation(
                displayInfo = WrpDisplayInfo("Relying party", null, "AT", null, null),
                certificates = listOf(WrprcCertificateValidation(valid = it)),
                requestDataValidationResults = listOf(
                    WrpRequestDataValidation(query, RequestDataValidity(true, allowed))
                ),
            )
        },
    )

    private fun multiSigned(vararg identities: WrpValidationResult, uncovered: List<String> = emptyList()) =
        WrpValidationResult(
            accessCertificate = null,
            registrationCertificate = null,
            multiSigned = true,
            signers = identities.mapIndexed { index, it -> SignerWrpValidation(index, "x509_hash:signer$index", it) },
            uncoveredCredentialQueryIds = uncovered,
        )

    private fun WrpValidationResult.failed() =
        (toWrprcRequestValidationGroups().flatMap { it.rows } + toRequestValidationData()).any { it.validity == false }

    @Test
    fun allEuSignersValidIsValid() {
        val result = multiSigned(identity(), identity())

        assertFalse(result.failed())
        assertEquals(2, result.toWrprcRequestValidationGroups().size)
        assertEquals(listOf("x509_hash:signer0", "x509_hash:signer1"), result.toWrprcRequestValidationGroups().map { it.clientId })
    }

    @Test
    fun signerWithACopiedRegistrationCertificateFailsTheWholeRequest() {
        // authenticated with a self-signed certificate, so not a valid WRPAC, next to a genuine WRPRC
        val result = multiSigned(identity(), identity(wrpIdentifier = null, validWrprc = false))

        assertTrue(result.failed())
        val copied = result.toWrprcRequestValidationGroups()[1]
        // its relying party details are not shown, the WRPRC is not bound to a valid access certificate
        assertNull(copied.displayInfo)
    }

    @Test
    fun signerOfAnotherTrustFrameworkIsNeutral() {
        val result = multiSigned(identity(), identity(wrpIdentifier = null, validWrprc = null))

        assertFalse(result.failed())
        assertEquals(1, result.eudiSigners.size)
        assertEquals(listOf("x509_hash:signer0"), result.toWrprcRequestValidationGroups().map { it.clientId })
    }

    @Test
    fun requestWithoutEuSignersShowsNothing() {
        val result = multiSigned(identity(wrpIdentifier = null, validWrprc = null))

        assertTrue(result.nothingProvided)
        assertFalse(multiSigned(identity()).nothingProvided)
        assertFalse(
            WrpValidationResult(
                accessCertificate = null,
                registrationCertificate = null,
                multiSigned = true,
                signersError = "too many signatures",
            ).nothingProvided
        )
    }

    @Test
    fun signersOfDifferentRelyingPartiesFail() {
        val result = multiSigned(identity(wrpIdentifier = "WRP-1"), identity(wrpIdentifier = "WRP-2"))

        assertTrue(result.differentRelyingParties)
        assertTrue(result.failed())
    }

    @Test
    fun credentialQueryWithoutRegistrationIsOnlyPointedOut() {
        // e.g. a credential of another trust framework, which a multisigned request may ask for
        listOf(
            multiSigned(identity(), uncovered = listOf("other")),
            identity().copy(uncoveredCredentialQueryIds = listOf("other")),
        ).forEach { result ->
            assertFalse(result.failed())
            val row = result.toRequestValidationData().single()
            assertNull(row.validity)
            assertEquals(listOf(WrpValidationError.Message("other: not covered by a registration certificate")), row.errors)
        }
    }

    @Test
    fun attributeIsOnlyAllowedIfEveryApplyingRegistrationCertificateAllowsIt() {
        val result = multiSigned(
            identity(allowed = listOf(givenName to true, familyName to true)),
            identity(allowed = listOf(givenName to true, familyName to false)),
        )

        assertEquals(listOf(givenName to true, familyName to false), result.allowedAttributes("pid"))
        assertNull(result.allowedAttributes("unknown"))
    }

    @Test
    fun singleSignedRequestIsOneGroupWithItsClientIdAndDetails() {
        val result = identity().copy(clientId = "x509_hash:verifier")
        val group = result.toWrprcRequestValidationGroups().single()

        assertEquals("x509_hash:verifier", group.clientId)
        assertEquals("Relying party", group.displayInfo?.name)
        assertFalse(result.failed())
    }

    @Test
    fun credentialQueriesNoRegistrationCertificateAppliesToAreUncovered() {
        val other = DCQLSdJwtCredentialQuery(
            id = DCQLCredentialQueryIdentifier("other"),
            meta = DCQLSdJwtCredentialMetadataAndValidityConstraints(vctValues = listOf("urn:other:1")),
        )
        val dcql = DCQLQuery(credentials = DCQLCredentialQueryList(nonEmptyListOf(query.query, other)))
        val registrationCertificate = WrprcValidationResult(
            certificateValidationResults = emptyMap(),
            requestDataValidationResults = listOf(query to KmmResult.success(RequestDataValidity(true, emptyList()))),
        )

        assertEquals(listOf("other"), uncoveredCredentialQueryIds(dcql, listOf(registrationCertificate)))
        // without registration certificate there is nothing to cover the queries
        assertEquals(emptyList(), uncoveredCredentialQueryIds(dcql, emptyList()))
    }

    @Test
    fun failedChecksAreTechnicalDetailsOfTheirRows() {
        val base = identity(allowed = listOf(givenName to true, familyName to false))
        val result = base.copy(
            registrationCertificate = base.registrationCertificate!!.copy(
                certificates = listOf(
                    WrprcCertificateValidation(
                        valid = false,
                        failedChecks = WrpRegistrationCertificateValidation(
                            validHeader = true,
                            validSignature = true,
                            validChain = false,
                            validPayload = true,
                            validLinkage = true,
                            validStatusList = true,
                        ).failedChecks(),
                    )
                ),
            )
        )

        val rows = result.toWrprcRequestValidationData()

        assertEquals(listOf(WrpValidationError.Message("Certificate chain not trusted")), rows[1].errors)
        assertEquals(
            listOf(WrpValidationError.Message("pid: eu.europa.ec.eudi.pid.1/family_name not registered")),
            rows[3].errors,
        )
    }
}

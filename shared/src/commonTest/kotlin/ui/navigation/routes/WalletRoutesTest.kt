package ui.navigation.routes

import at.asitplus.iso.DocRequest
import at.asitplus.iso.ItemsRequest
import at.asitplus.iso.ItemsRequestList
import at.asitplus.iso.SingleItemsRequest
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.LoadingMessageKey
import at.asitplus.wallet.app.common.dcapi.isoMdocDcApiRequest
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyValidationUi
import at.asitplus.wallet.app.common.relyingParty.WrpDisplayInfo
import at.asitplus.wallet.app.common.validation.ArtifactKind
import at.asitplus.wallet.app.common.validation.ArtifactUi
import at.asitplus.wallet.app.common.validation.CheckState
import at.asitplus.wallet.app.common.validation.CheckUi
import at.asitplus.wallet.app.common.validation.FindingUi
import at.asitplus.wallet.app.common.validation.HeadlineUi
import at.asitplus.wallet.app.common.validation.UnregisteredRequestUi
import at.asitplus.wallet.app.common.validation.ValidationOutcomeUi
import at.asitplus.wallet.lib.agent.validation.relyingParty.ReaderAuthenticationVerifier
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.RequestDataValidity
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpCredentialRequest
import at.asitplus.wallet.lib.data.JsonClaimReference
import at.asitplus.wallet.lib.data.MdocClaimReference
import at.asitplus.wallet.lib.openid.IsoMdocDcapiResponseBuilder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ui.viewmodels.QrCodeScannerMode

class WalletRoutesTest {
    @Test
    fun qrCredentialOfferRouteRequiresSameCapabilitiesAsAddCredentialRoute() {
        val route: Route = AddCredentialPreAuthnRoute("serialized-offer")

        assertTrue(route is PrerequisiteRoute)
        assertEquals(AddCredentialRoute.prerequisites, route.prerequisites)
    }

    @Test
    fun dcApiCredentialOfferRouteRequiresSameCapabilitiesAsAddCredentialRoute() {
        val route: Route = AddCredentialDcApiRoute("serialized-offer")

        assertTrue(route is PrerequisiteRoute)
        assertEquals(AddCredentialRoute.prerequisites, route.prerequisites)
    }

    @Test
    fun qrScannerRoutesRequireCredentialLoadingCapabilitiesAndCamera() {
        val expectedPrerequisites = AddCredentialRoute.prerequisites + RoutePrerequisites.CAMERA

        QrCodeScannerMode.entries.forEach { mode ->
            assertEquals(
                expectedPrerequisites,
                QrCodeScannerRoute(mode).prerequisites,
            )
        }
    }

    @Test
    fun loadingRouteSerializesMessageKey() {
        val route = LoadingRoute(LoadingMessageKey.IssuerMetadata)

        val serialized = joseCompliantSerializer.encodeToString(route)
        val deserialized = joseCompliantSerializer.decodeFromString<LoadingRoute>(serialized)

        assertEquals(LoadingMessageKey.IssuerMetadata.name, deserialized.message)
        assertEquals(LoadingMessageKey.IssuerMetadata, deserialized.messageKey)
    }

    @Test
    fun dcApiPresentationRoutePreservesGenericRequest() {
        val parameters = AuthenticationRequestParameters(nonce = "test-nonce")
        val request = RequestParametersFrom.OpenId4VpDcApiUnsigned(
            parameters = parameters,
            jsonString = joseCompliantSerializer.encodeToString(parameters),
            credentialIds = listOf("test-credential"),
            callingPackageName = "com.example.verifier",
            callingOrigin = "https://verifier.example.com",
        )

        val route = DCAPIPresentationViewRoute(request)

        assertEquals(request, route.request)
    }

    @Test
    fun dcApiPresentationRouteKeepsTheSignedIsoMdocRequest() = runTest {
        val request = isoMdocDcApiRequest(AGE_VERIFICATION_ZK_REQUEST, callingOrigin = AGE_VERIFICATION_ORIGIN)

        val routed = DCAPIPresentationViewRoute(request).request as RequestParametersFrom.IsoMdocDcApi

        assertEquals(AGE_VERIFICATION_ZK_REQUEST, routed.jsonString)
        val reader = ReaderAuthenticationVerifier().authenticate(
            routed.parameters.isoMdocRequest.deviceRequest,
            IsoMdocDcapiResponseBuilder.sessionTranscriptFor(routed),
        )
        assertTrue(reader.isSuccess, reader.exceptionOrNull()?.toString())
    }

    @Test
    fun relyingPartyValidationOfIsoMdocRequestSurvivesSerialization() {
        val docType = "eu.europa.ec.eudi.pid.1"
        val docRequest = DocRequest(
            itemsRequest = ByteStringWrapper(
                ItemsRequest(
                    docType = docType,
                    namespaces = mapOf(
                        docType to ItemsRequestList(listOf(SingleItemsRequest("given_name", intentToRetain = false)))
                    ),
                )
            )
        )
        val requestDataValidity = RequestDataValidity(
            credentialTypeValidity = true,
            credentialAttributesValidity = listOf(
                MdocClaimReference(docType, "given_name") to true,
                // NormalizedJsonPath is a list, which must not break the polymorphic serialization of the reference
                JsonClaimReference(NormalizedJsonPath() + "address" + 0u) to false,
            ),
        )
        val unregistered = UnregisteredRequestUi(WrpCredentialRequest.WrpDocRequest(docRequest), requestDataValidity)
        val registrationCertificate = ArtifactUi(
            kind = ArtifactKind.REGISTRATION_CERTIFICATE,
            accepted = false,
            checks = listOf(
                CheckUi(
                    name = "requestAuthorization",
                    state = CheckState.FAILED,
                    satisfied = false,
                    tolerable = true,
                    finding = FindingUi.Unregistered(listOf(unregistered)),
                )
            ),
            children = emptyList(),
        )
        val validation = RelyingPartyValidationUi(
            outcome = ValidationOutcomeUi(
                accepted = false,
                integrityFailed = false,
                headline = listOf(HeadlineUi(ArtifactKind.REGISTRATION_CERTIFICATE, registrationCertificate.checks.single())),
                root = ArtifactUi(ArtifactKind.RELYING_PARTY, false, emptyList(), listOf(registrationCertificate)),
                evaluatedAtEpochMillis = 0,
            ),
            registration = WrpDisplayInfo("Verifier", null, "AT", null, null),
        )

        val decoded = joseCompliantSerializer.decodeFromString<RelyingPartyValidationUi>(
            joseCompliantSerializer.encodeToString(validation)
        )

        assertEquals(validation, decoded)
        assertEquals(listOf(unregistered), decoded.unregisteredRequests)
        assertFalse(decoded.registrationVerified)
    }
}

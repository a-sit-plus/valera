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
import at.asitplus.wallet.app.common.relyingParty.WrpValidationResult
import at.asitplus.wallet.app.common.relyingParty.WrpRequestDataValidation
import at.asitplus.wallet.app.common.relyingParty.WrpTokenStatus
import at.asitplus.wallet.app.common.relyingParty.WrprcCertificateValidation
import at.asitplus.wallet.app.common.relyingParty.WrprcValidation
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.RequestDataValidity
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpCredentialRequest
import at.asitplus.wallet.lib.data.JsonClaimReference
import at.asitplus.wallet.lib.data.MdocClaimReference
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun dcApiPresentationRoutePreservesIsoMdocWrpValidationResult() {
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
        val wrpValidationResult = WrpValidationResult(
            accessCertificate = null,
            accessCertificateError = "untrusted WRPAC",
            registrationCertificate = WrprcValidation(
                displayInfo = null,
                certificates = listOf(
                    WrprcCertificateValidation(valid = false, validLinkage = false, tokenStatus = WrpTokenStatus.REVOKED)
                ),
                requestDataValidationResults = listOf(
                    WrpRequestDataValidation(WrpCredentialRequest.WrpDocRequest(docRequest), requestDataValidity)
                ),
            ),
        )
        val parameters = AuthenticationRequestParameters(nonce = "test-nonce")
        val request = RequestParametersFrom.OpenId4VpDcApiUnsigned(
            parameters = parameters,
            jsonString = joseCompliantSerializer.encodeToString(parameters),
            credentialIds = listOf("test-credential"),
            callingPackageName = "com.example.verifier",
            callingOrigin = "https://verifier.example.com",
        )

        val route = DCAPIPresentationViewRoute(request, wrpValidationResult)

        assertEquals(wrpValidationResult, route.wrpValidationResult)
    }
}

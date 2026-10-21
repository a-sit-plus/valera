package at.asitplus.wallet.app.common.presentation

import at.asitplus.iso.DeviceRequest
import at.asitplus.iso.DocRequest
import at.asitplus.iso.DocRequestInfo
import at.asitplus.iso.ItemsRequest
import at.asitplus.iso.ItemsRequestList
import at.asitplus.iso.SingleItemsRequest
import at.asitplus.iso.ZkRequest
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLCredentialQueryList
import at.asitplus.openid.dcql.DCQLIsoMdocCredentialMetadataAndValidityConstraints
import at.asitplus.openid.dcql.DCQLIsoMdocCredentialQuery
import at.asitplus.openid.dcql.DCQLIsoMdocZkCredentialMetadataAndValidityConstraints
import at.asitplus.openid.dcql.DCQLIsoMdocZkCredentialQuery
import at.asitplus.openid.dcql.DCQLIsoMdocZkSystemType
import at.asitplus.openid.dcql.DCQLQuery
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.zk.iso.IsoMdocZkBackendRegistry
import io.github.z4kn4fein.semver.Version
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LongfellowZkRegistrationTest {
    @Test
    fun registersLongfellowBackendOnce() = runTest {
        LongfellowZkRegistration.ensureRegistered()
        LongfellowZkRegistration.ensureRegistered()

        val backends = IsoMdocZkBackendRegistry.Default.backends.filter { it.system.startsWith("longfellow") }
        assertEquals(1, backends.size)
    }

    @Test
    fun deviceRequestNeedsZkBackendOnlyWithZkRequest() {
        assertFalse(deviceRequest(zkRequest = null).toCredentialPresentation().requestsZkProof())
        assertTrue(
            deviceRequest(ZkRequest(zkRequired = false, systemSpecs = emptyList()))
                .toCredentialPresentation().requestsZkProof()
        )
    }

    @Test
    fun dcqlQueryNeedsZkBackendOnlyWithZkCredentialQuery() {
        val plain = DCQLIsoMdocCredentialQuery(
            id = DCQLCredentialQueryIdentifier("plain"),
            meta = DCQLIsoMdocCredentialMetadataAndValidityConstraints(doctypeValue = DOC_TYPE),
        )
        val zk = DCQLIsoMdocZkCredentialQuery(
            id = DCQLCredentialQueryIdentifier("zk"),
            meta = DCQLIsoMdocZkCredentialMetadataAndValidityConstraints(
                doctypeValue = DOC_TYPE,
                zkSystemType = DCQLIsoMdocZkSystemType(systemSpecs = emptyList()),
            ),
        )

        assertFalse(dcqlRequest(DCQLCredentialQueryList(plain)).toCredentialPresentation().requestsZkProof())
        assertTrue(dcqlRequest(DCQLCredentialQueryList(plain, zk)).toCredentialPresentation().requestsZkProof())
    }

    private fun dcqlRequest(credentials: DCQLCredentialQueryList<*>) =
        CredentialPresentationRequest.DCQLRequest(DCQLQuery(credentials = credentials))

    private fun deviceRequest(zkRequest: ZkRequest?) = CredentialPresentationRequest.IsoDeviceRetrieval(
        DeviceRequest(
            parsedVersion = Version(1, 0),
            docRequests = arrayOf(
                DocRequest(
                    itemsRequest = ByteStringWrapper(
                        ItemsRequest(
                            docType = DOC_TYPE,
                            namespaces = mapOf(DOC_TYPE to ItemsRequestList(listOf(SingleItemsRequest("age_over_18", false)))),
                            requestInfo = zkRequest?.let { DocRequestInfo(zkRequest = it) },
                        )
                    )
                )
            )
        )
    )

    private companion object {
        const val DOC_TYPE = "org.iso.18013.5.1.mDL"
    }
}

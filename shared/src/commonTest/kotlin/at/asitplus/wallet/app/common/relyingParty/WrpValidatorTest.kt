package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.data.NonEmptyList.Companion.nonEmptyListOf
import at.asitplus.dcapi.request.IsoMdocRequest
import at.asitplus.iso.DeviceRequest
import at.asitplus.iso.DocRequest
import at.asitplus.iso.DocRequestInfo
import at.asitplus.iso.EncryptionInfo
import at.asitplus.iso.EncryptionParameters
import at.asitplus.iso.ItemsRequest
import at.asitplus.iso.ItemsRequestList
import at.asitplus.iso.SingleItemsRequest
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.OpenIdConstants.VerifierInfo.REGISTRATION_CERT_FORMAT
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.openid.VerifierInfo
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.signum.indispensable.cosef.toCoseKey
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import io.github.z4kn4fein.semver.Version
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WrpValidatorTest {
    @Test
    fun unsignedRequestWithoutVerifierInfoHasNoCertificates() = runTest {
        val request = dcApiRequest(AuthenticationRequestParameters(nonce = "nonce"))

        assertNull(request.toWrpacRequestData())
        assertEquals(false, request.hasRegistrationCertificate())
    }

    @Test
    fun registrationCertificateIsPresentEvenIfItCanNotBeParsed() {
        val request = dcApiRequest(
            AuthenticationRequestParameters(
                nonce = "nonce",
                verifierInfo = nonEmptyListOf(VerifierInfo(REGISTRATION_CERT_FORMAT, "not-a-jws")),
            )
        )

        assertEquals(true, request.hasRegistrationCertificate())
    }

    @Test
    fun isoRequestWithoutReaderAuthAndWrprcHasNoCertificates() = runTest {
        val request = isoRequest(docRequest(euWrprc = null))

        assertNull(request.toWrpacRequestData())
        assertEquals(false, request.hasRegistrationCertificate())
    }

    @Test
    fun isoRequestWithWrprcHasRegistrationCertificate() = runTest {
        val request = isoRequest(docRequest(euWrprc = byteArrayOf(1, 2, 3)))

        assertEquals(true, request.hasRegistrationCertificate())
    }

    private fun dcApiRequest(parameters: AuthenticationRequestParameters) =
        RequestParametersFrom.OpenId4VpDcApiUnsigned(
            parameters = parameters,
            jsonString = joseCompliantSerializer.encodeToString(parameters),
            credentialIds = emptyList(),
            callingPackageName = "com.example.verifier",
            callingOrigin = "https://verifier.example.com",
        )

    private fun docRequest(euWrprc: ByteArray?) = DocRequest(
        itemsRequest = ByteStringWrapper(
            ItemsRequest(
                docType = DOC_TYPE,
                namespaces = mapOf(
                    DOC_TYPE to ItemsRequestList(listOf(SingleItemsRequest("given_name", intentToRetain = false)))
                ),
                requestInfo = euWrprc?.let { DocRequestInfo(euWrprc = it) },
            )
        )
    )

    private suspend fun isoRequest(docRequest: DocRequest) = RequestParametersFrom.IsoMdocDcApi(
        parameters = RequestParametersFrom.IsoMdocDcApi.IsoMdocRequestWrapper(
            IsoMdocRequest(
                DeviceRequest(Version(1, 1), docRequests = arrayOf(docRequest)),
                EncryptionInfo(
                    "dcapi",
                    EncryptionParameters(
                        recipientPublicKey = EphemeralKeyWithoutCert().publicKey.toCoseKey().getOrThrow()
                    )
                ),
            )
        ),
        jsonString = "",
        callingOrigin = "https://verifier.example.com",
    )

    private companion object {
        const val DOC_TYPE = "eu.europa.ec.eudi.pid.1"
    }
}

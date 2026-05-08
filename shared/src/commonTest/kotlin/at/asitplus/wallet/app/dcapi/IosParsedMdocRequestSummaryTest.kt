package at.asitplus.wallet.app.dcapi

import at.asitplus.dcapi.DCAPIHandover
import at.asitplus.dcapi.request.IsoMdocRequest
import at.asitplus.iso.DeviceRequest
import at.asitplus.iso.DocRequest
import at.asitplus.iso.EncryptionInfo
import at.asitplus.iso.EncryptionParameters
import at.asitplus.iso.ItemsRequest
import at.asitplus.iso.ItemsRequestList
import at.asitplus.iso.SingleItemsRequest
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment.NameSegment
import at.asitplus.signum.indispensable.cosef.CoseEllipticCurve
import at.asitplus.signum.indispensable.cosef.CoseKey
import at.asitplus.signum.indispensable.cosef.CoseKeyParams
import at.asitplus.signum.indispensable.cosef.CoseKeyType
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosParsedMdocRequestSummaryTest {
    @Test
    fun exactMatchIsConsistent() {
        val rawRequest = rawRequest(
            "org.iso.18013.5.1.mDL" to mapOf(
                "org.iso.18013.5.1" to mapOf(
                    "family_name" to false,
                    "given_name" to false
                )
            )
        )

        val summary = IosParsedMdocRequestSummary(
            documentRequests = listOf(
                IosParsedMdocDocumentRequest(
                    docType = "org.iso.18013.5.1.mDL",
                    namespaces = mapOf(
                        "org.iso.18013.5.1" to mapOf(
                            "family_name" to false,
                            "given_name" to false
                        )
                    )
                )
            )
        )

        assertTrue(summary.isConsistentWith(rawRequest))
    }

    @Test
    fun orderingDifferencesAreStillConsistent() {
        val rawRequest = rawRequest(
            "org.iso.18013.5.1.mDL" to mapOf(
                "org.iso.18013.5.1" to mapOf(
                    "family_name" to false,
                    "given_name" to false
                )
            ),
            "eu.europa.ec.eudi.pid.1" to mapOf(
                "eu.europa.ec.eudi.pid.1" to mapOf(
                    "birth_date" to true
                )
            )
        )

        val summary = IosParsedMdocRequestSummary(
            documentRequests = listOf(
                IosParsedMdocDocumentRequest(
                    docType = "eu.europa.ec.eudi.pid.1",
                    namespaces = mapOf(
                        "eu.europa.ec.eudi.pid.1" to mapOf("birth_date" to true)
                    )
                ),
                IosParsedMdocDocumentRequest(
                    docType = "org.iso.18013.5.1.mDL",
                    namespaces = mapOf(
                        "org.iso.18013.5.1" to mapOf(
                            "given_name" to false,
                            "family_name" to false
                        )
                    )
                )
            )
        )

        assertTrue(summary.isConsistentWith(rawRequest))
    }

    @Test
    fun changedRequestIsInconsistent() {
        val rawRequest = rawRequest(
            "org.iso.18013.5.1.mDL" to mapOf(
                "org.iso.18013.5.1" to mapOf(
                    "family_name" to false,
                    "given_name" to false
                )
            )
        )

        val summary = IosParsedMdocRequestSummary(
            documentRequests = listOf(
                IosParsedMdocDocumentRequest(
                    docType = "org.iso.18013.5.1.mDL",
                    namespaces = mapOf(
                        "org.iso.18013.5.1" to mapOf(
                            "family_name" to false,
                            "portrait" to false
                        )
                    )
                )
            )
        )

        assertFalse(summary.isConsistentWith(rawRequest))
    }

    @Test
    fun summaryConvertsToDifInputDescriptors() {
        val summary = IosParsedMdocRequestSummary(
            documentRequests = listOf(
                IosParsedMdocDocumentRequest(
                    docType = "org.iso.18013.5.1.mDL",
                    namespaces = mapOf(
                        "org.iso.18013.5.1" to mapOf(
                            "family_name" to false,
                            "given_name" to true
                        )
                    )
                )
            )
        )

        val descriptors = summary.toDifInputDescriptors()

        assertEquals(1, descriptors.size)
        assertEquals("org.iso.18013.5.1.mDL", descriptors.single().id)
        val fields = descriptors.single().constraints?.fields.orEmpty()
        val familyNamePath = NormalizedJsonPath(
            NameSegment("org.iso.18013.5.1"),
            NameSegment("family_name"),
        ).toString()
        val givenNamePath = NormalizedJsonPath(
            NameSegment("org.iso.18013.5.1"),
            NameSegment("given_name"),
        ).toString()
        assertEquals(2, fields.size)
        assertEquals(
            setOf(familyNamePath, givenNamePath),
            fields.map { it.path.single() }.toSet()
        )
        assertEquals(
            mapOf(
                familyNamePath to false,
                givenNamePath to true
            ),
            fields.associate { it.path.single() to it.intentToRetain }
        )
    }

    private fun rawRequest(vararg docs: Pair<String, Map<String, Map<String, Boolean>>>) = IsoMdocRequest(
        deviceRequest = DeviceRequest(
            version = "1.0",
            docRequests = docs.map { (docType, namespaces) ->
                DocRequest(
                    itemsRequest = ByteStringWrapper(
                        ItemsRequest(
                            docType = docType,
                            namespaces = namespaces.mapValues { (_, elements) ->
                                ItemsRequestList(
                                    elements.entries.map { (element, intentToRetain) ->
                                        SingleItemsRequest(element, intentToRetain)
                                    }
                                )
                            }
                        )
                    )
                )
            }.toTypedArray()
        ),
        encryptionInfo = EncryptionInfo(
            type = DCAPIHandover.Companion.TYPE_DCAPI,
            encryptionParameters = EncryptionParameters(
                recipientPublicKey = CoseKey(
                    type = CoseKeyType.EC2,
                    keyParams = CoseKeyParams.EcYBoolParams(curve = CoseEllipticCurve.P256)
                )
            )
        )
    )
}

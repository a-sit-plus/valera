package at.asitplus.wallet.app.dcapi

import at.asitplus.dcapi.request.IsoMdocRequest
import at.asitplus.dif.Constraint
import at.asitplus.dif.ConstraintField
import at.asitplus.dif.DifInputDescriptor
import at.asitplus.dif.FormatContainerJwt
import at.asitplus.dif.FormatHolder
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment.NameSegment
import kotlinx.serialization.Serializable

@Serializable
data class IosParsedMdocRequestSummary(
    val documentRequests: List<IosParsedMdocDocumentRequest>
) {
    fun isConsistentWith(rawRequest: IsoMdocRequest): Boolean =
        normalizedDocumentRequests() == rawRequest.normalizedDocumentRequests()

    fun toDifInputDescriptors(): List<DifInputDescriptor> =
        documentRequests.map { request ->
            DifInputDescriptor(
                id = request.docType,
                format = FormatHolder(msoMdoc = FormatContainerJwt()),
                constraints = Constraint(
                    fields = request.namespaces.flatMap { (namespace, elements) ->
                        elements.map { (element, intentToRetain) ->
                            ConstraintField(
                                path = listOf(
                                    NormalizedJsonPath(
                                        NameSegment(namespace),
                                        NameSegment(element),
                                    ).toString()
                                ),
                                intentToRetain = intentToRetain
                            )
                        }
                    }.toSet()
                )
            )
        }

    private fun normalizedDocumentRequests(): List<NormalizedDocumentRequest> =
        documentRequests.map { it.normalize() }.sorted()
}

@Serializable
data class IosParsedMdocDocumentRequest(
    val docType: String,
    val namespaces: Map<String, Map<String, Boolean>>
) {
    fun normalize(): NormalizedDocumentRequest =
        NormalizedDocumentRequest(
            docType = docType,
            namespaces = namespaces.entries
                .sortedBy { it.key }
                .associate { (namespace, elements) ->
                    namespace to elements.entries
                        .sortedBy { it.key }
                        .associate { it.key to it.value }
                }
        )
}

private fun IsoMdocRequest.normalizedDocumentRequests(): List<NormalizedDocumentRequest> =
    deviceRequest.docRequests.map { docRequest ->
        NormalizedDocumentRequest(
            docType = docRequest.itemsRequest.value.docType,
            namespaces = docRequest.itemsRequest.value.namespaces.entries
                .sortedBy { it.key }
                .associate { (namespace, items) ->
                    namespace to items.entries
                        .map { it.dataElementIdentifier to it.intentToRetain }
                        .sortedBy { it.first }
                        .associate { it.first to it.second }
                }
        )
    }.sorted()

data class NormalizedDocumentRequest(
    val docType: String,
    val namespaces: Map<String, Map<String, Boolean>>
) : Comparable<NormalizedDocumentRequest> {
    override fun compareTo(other: NormalizedDocumentRequest): Int =
        compareValuesBy(this, other, NormalizedDocumentRequest::docType, { it.namespaces.toString() })
}

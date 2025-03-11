package data.document

import kotlinx.serialization.Serializable

@Serializable
data class RequestDocument(
    val docType: String,
    var itemsToRequest: Map<String, Map<String, Boolean>>
)

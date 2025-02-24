package data.bletransfer.util

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val docType: String,
    val requestDocument: Map<String, Map<String, Boolean>>
)

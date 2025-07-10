package data.document
import kotlinx.serialization.Serializable

@Serializable
class RequestDocumentList {
    private val list = mutableListOf<RequestDocument>()

    fun addRequestDocument(requestDocument: RequestDocument) { list.add(requestDocument) }

    fun getAll(): List<RequestDocument> = list
}

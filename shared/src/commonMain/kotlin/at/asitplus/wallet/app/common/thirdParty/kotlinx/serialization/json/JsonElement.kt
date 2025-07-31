package at.asitplus.wallet.app.common.thirdParty.kotlinx.serialization.json

import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.NodeListEntry
import at.asitplus.jsonpath.core.NormalizedJsonPath
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

fun JsonElement.normalizedJsonPaths(): List<NormalizedJsonPath> = leafNodeList().map {
    it.normalizedJsonPath
}

fun JsonElement.leafNodeList(): NodeList = when (this) {
    is JsonArray -> jsonArray.flatMapIndexed { index: Int, jsonElement: JsonElement ->
        jsonElement.leafNodeList().map {
            NodeListEntry(
                NormalizedJsonPath() + index.toUInt() + it.normalizedJsonPath,
                it.value
            )
        }
    }

    is JsonObject -> jsonObject.entries.flatMap { (path, value) ->
        value.leafNodeList().map {
            NodeListEntry(
                NormalizedJsonPath() + path + it.normalizedJsonPath,
                it.value,
            )
        }
    }

    else -> listOf(NodeListEntry(NormalizedJsonPath(), this))
}

package at.asitplus.wallet.app.common.thirdParty.kotlinx.serialization.json

import at.asitplus.jsonpath.core.NormalizedJsonPath
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.jsonpath.core.plus

fun JsonElement.normalizedJsonPaths(): List<NormalizedJsonPath> {
    return when(this) {
        is JsonArray -> jsonArray.flatMapIndexed { index: Int, jsonElement: JsonElement ->
            jsonElement.normalizedJsonPaths().map {
                NormalizedJsonPath() + index.toUInt() + it
            }
        }

        is JsonObject -> jsonObject.entries.flatMap { entry ->
            entry.value.normalizedJsonPaths().map {
                NormalizedJsonPath() + entry.key + it
            }
        }

        else -> listOf(NormalizedJsonPath())
    }
}
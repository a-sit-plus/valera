package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.jsonpath.core.NormalizedJsonPath
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class SdJwtEntryTest {
    private fun export(json: String): SdJwtEntry = SdJwtEntry.fromJson(
        jwtId = "synthetic-document",
        verifiableCredentialType = "urn:example:synthetic",
        claims = Json.parseToJsonElement(json).jsonObject,
        attributeLabel = { path ->
            when (path) {
                NormalizedJsonPath() + "family_name" -> "Family name"
                NormalizedJsonPath() + "addresses" + "country" -> "Country"
                else -> null
            }
        },
    )

    @Test
    fun exportsRealParentsEmptyContainersAndNull() {
        val root = export("""{
            "place_of_birth":{"locality":"Wien","country":"AT"},
            "emptyObject":{},"emptyArray":[],"absentValue":null
        }""").claimsTree

        assertEquals("object", root.kind)
        assertEquals(setOf("place_of_birth", "emptyObject", "emptyArray", "absentValue"), root.members.keys)
        val parent = root.members.getValue("place_of_birth")
        assertEquals("object", parent.kind)
        assertEquals("Wien", parent.members.getValue("locality").value)
        assertEquals("", parent.value)
        assertEquals("object", root.members.getValue("emptyObject").kind)
        assertTrue(root.members.getValue("emptyObject").members.isEmpty())
        assertEquals("array", root.members.getValue("emptyArray").kind)
        assertTrue(root.members.getValue("emptyArray").elements.isEmpty())
        assertEquals("null", root.members.getValue("absentValue").kind)
        assertEquals("", root.members.getValue("absentValue").value)
    }

    @Test
    fun preservesNestedArraysWithoutWildcardProjections() {
        val root = export("""{
            "addresses":[{"country":"AT"},{"country":"DE"}],
            "matrix":[[{"value":"nested"}],[],[null]]
        }""").claimsTree

        val addresses = root.members.getValue("addresses")
        assertEquals("array", addresses.kind)
        assertTrue(addresses.members.isEmpty())
        assertEquals(listOf("AT", "DE"), addresses.elements.map { it.members.getValue("country").value })
        assertEquals("Country", addresses.elements.first().members.getValue("country").displayName)
        val matrix = root.members.getValue("matrix")
        assertEquals("array", matrix.elements.first().kind)
        assertEquals("nested", matrix.elements.first().elements.first().members.getValue("value").value)
        assertTrue(matrix.elements[1].elements.isEmpty())
        assertEquals("null", matrix.elements[2].elements.single().kind)
    }

    @Test
    fun preservesLiteralMemberNamesWithoutPathCollisions() {
        val root = export("""{
            "a.b":"literal","a":{"b":"nested"},
            "0":"zero","null":"literal null","":"empty name",
            "items":[{"0":"object member","null":"member"}]
        }""").claimsTree

        assertEquals("literal", root.members.getValue("a.b").value)
        assertEquals("nested", root.members.getValue("a").members.getValue("b").value)
        assertEquals("zero", root.members.getValue("0").value)
        assertEquals("literal null", root.members.getValue("null").value)
        assertEquals("empty name", root.members.getValue("").value)
        assertEquals("object member", root.members.getValue("items").elements.single().members.getValue("0").value)
    }

    @Test
    fun retainsExactPrimitiveTypesAndNumericLexicalValues() {
        val root = export("""{
            "number":9007199254740993,"decimal":1.2300e+4,"boolean":true,
            "falseBoolean":false,"stringNumber":"9007199254740993",
            "stringBoolean":"true","stringNull":"null","emptyString":""
        }""").claimsTree

        assertEquals("number", root.members.getValue("number").kind)
        assertEquals("9007199254740993", root.members.getValue("number").value)
        assertEquals("1.2300e+4", root.members.getValue("decimal").value)
        assertEquals("boolean", root.members.getValue("boolean").kind)
        assertEquals("true", root.members.getValue("boolean").value)
        assertEquals("false", root.members.getValue("falseBoolean").value)
        listOf("stringNumber", "stringBoolean", "stringNull", "emptyString").forEach {
            assertEquals("string", root.members.getValue(it).kind)
        }
        assertEquals("", root.members.getValue("emptyString").value)
    }

    @Test
    fun truncatesOnlyDisplayValuesAndUsesMetadataLabels() {
        val longValue = "x".repeat(300)
        val root = export("""{"family_name":"$longValue"}""").claimsTree
        val child = root.members.getValue("family_name")

        assertEquals("Family name", child.displayName)
        assertEquals(longValue, child.value)
        assertEquals("x".repeat(128) + "...", child.displayValue)
        assertTrue(root.displayValue.length < longValue.length)
    }

    @Test
    fun cborRoundTripPreservesTreeAndRequiredVersionWithoutEncodingDefaults() {
        val entry = export("""{"object":{},"array":[],"null":null,"empty":"","nested":[[1,true,"1"]]}""")
        val cbor = Cbor { encodeDefaults = false }
        val bytes = cbor.encodeToByteArray(entry)
        assertEquals(entry, cbor.decodeFromByteArray<SdJwtEntry>(bytes))

        // JSON uses the same generated serializer descriptor and exposes exact wire field names.
        val wire = Json { encodeDefaults = false }.encodeToJsonElement(SdJwtEntry.serializer(), entry).jsonObject
        assertEquals(setOf("documentId", "vct", "claimsFormatVersion", "claimsTree"), wire.keys)
        assertEquals("1", wire.getValue("claimsFormatVersion").jsonPrimitive.content)
        assertFalse("claims" in wire)
        val tree = wire.getValue("claimsTree").jsonObject
        assertEquals("object", tree.getValue("kind").jsonPrimitive.content)
        val members = tree.getValue("members").jsonObject
        assertEquals("object", members.getValue("object").jsonObject.getValue("kind").jsonPrimitive.content)
        assertFalse("members" in members.getValue("object").jsonObject)
        assertFalse("elements" in members.getValue("array").jsonObject)
        assertFalse("value" in members.getValue("null").jsonObject)
        assertEquals("string", members.getValue("empty").jsonObject.getValue("kind").jsonPrimitive.content)
    }
}

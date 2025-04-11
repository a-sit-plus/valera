package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import kotlinx.serialization.json.JsonElement

sealed interface CredentialStructureSpecificClaimReference

data class JsonClaimReference(
    val normalizedJsonPath: NormalizedJsonPath,
) : CredentialStructureSpecificClaimReference

data class MdocClaimReference(
    val namespace: String,
    val elementIdentifier: String,
) : CredentialStructureSpecificClaimReference
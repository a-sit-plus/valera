package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import kotlin.jvm.JvmInline

sealed interface SingleClaimReference

@JvmInline
value class JsonClaimReference(
    val normalizedJsonPath: NormalizedJsonPath,
) : SingleClaimReference

data class MdocClaimReference(
    val namespace: String,
    val claimName: String,
) : SingleClaimReference
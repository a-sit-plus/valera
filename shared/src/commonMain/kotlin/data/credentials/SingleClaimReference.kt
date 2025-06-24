package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import kotlin.jvm.JvmInline

sealed interface SingleClaimReference

@JvmInline
value class SdJwtClaimReference(
    val normalizedJsonPath: NormalizedJsonPath,
) : SingleClaimReference

data class MdocClaimReference(
    val namespace: String,
    val claimName: String,
) : SingleClaimReference
package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.signum.indispensable.cosef.io.Base16Strict
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString

internal fun Any.toCustomString(): String = when (this) {
    is ByteArray -> this.encodeToString(Base16Strict)
    is Array<*> -> this.contentDeepToString()
    else -> this.toString()
}

internal fun String.toJsonPath() = NormalizedJsonPath(
    NormalizedJsonPathSegment.NameSegment(this)
)

internal fun String.safeSubstring(len: Int) =
    if (this.length >= len) this.substring(0, len) + "..." else this
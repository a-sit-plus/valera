package at.asitplus.wallet.app.common.domain.vck.tokenStatusList

import at.asitplus.KmmResult
import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.CryptoPublicKey
import at.asitplus.signum.indispensable.CryptoSignature
import at.asitplus.signum.indispensable.SignatureAlgorithm
import at.asitplus.signum.supreme.sign.Verifier
import at.asitplus.wallet.lib.agent.VerifySignature
import at.asitplus.wallet.lib.agent.VerifySignatureFun

/**
 * Verifies a signature with [verifySignature], and if that fails for a COSE_Sign1 `Sig_structure`, verifies it again
 * with the entries of its protected header in deterministic order (RFC 8949, 4.2.1).
 *
 * Signum re-encodes the protected header of a received COSE_Sign1 in the order of its fields instead of using the bytes
 * as received (RFC 9052, 4.4), so that the signature of an issuer ordering the entries otherwise does not verify, e.g.
 * of the status list CWTs of the EUDI Wallet reference implementation with 1 (alg), 16 (type), 33 (x5chain). Only the
 * order of the entries changes, with their encoding kept, so a signature verifying either way covers the same header.
 *
 * TODO Remove once Signum verifies the protected header as received
 */
class DeterministicCoseHeaderVerifySignature(
    private val verifySignature: VerifySignatureFun = VerifySignature(),
) : VerifySignatureFun {
    override suspend fun invoke(
        input: ByteArray,
        signature: CryptoSignature,
        algorithm: SignatureAlgorithm,
        publicKey: CryptoPublicKey,
    ): KmmResult<Verifier.Success> {
        val result = verifySignature(input, signature, algorithm, publicKey)
        if (result.isSuccess) return result
        val reordered = input.withDeterministicProtectedHeader() ?: return result
        return verifySignature(reordered, signature, algorithm, publicKey).takeIf { it.isSuccess } ?: result
    }
}

private const val CONTEXT_SIGNATURE1 = "Signature1"

/**
 * [this] `Sig_structure` of a COSE_Sign1, `["Signature1", protected, external_aad, payload]`, with the entries of the
 * protected header sorted by their encoded keys, or `null` if it is no such structure or already in that order.
 */
internal fun ByteArray.withDeterministicProtectedHeader(): ByteArray? = catchingUnwrapped {
    val reader = CborReader(this)
    if (reader.readHead() != CborHead(MAJOR_ARRAY, 4)) return null
    if (reader.readHead() != CborHead(MAJOR_TEXT, CONTEXT_SIGNATURE1.length.toLong())) return null
    if (reader.readBytes(CONTEXT_SIGNATURE1.length).decodeToString() != CONTEXT_SIGNATURE1) return null
    val protectedHead = reader.readHead()
    if (protectedHead.major != MAJOR_BYTES) return null
    val headerStart = reader.position
    val headerEnd = headerStart + protectedHead.argument.toInt()
    val header = CborReader(copyOfRange(headerStart, headerEnd))
    val mapHead = header.readHead()
    if (mapHead.major != MAJOR_MAP) return null
    val mapHeadEnd = header.position
    val entries = List(mapHead.argument.toInt()) {
        val key = header.readItem()
        key to header.readItem()
    }
    if (header.position != headerEnd - headerStart) return null
    val sorted = entries.sortedWith { (a, _), (b, _) -> compareBytewise(a, b) }
    if (sorted == entries) return null
    copyOfRange(0, headerStart + mapHeadEnd) +
            sorted.flatMap { (key, value) -> listOf(key, value) }.reduce(ByteArray::plus) +
            copyOfRange(headerEnd, size)
}.getOrNull()

/** Bytewise lexicographic order of encoded keys, see RFC 8949, 4.2.1. */
private fun compareBytewise(a: ByteArray, b: ByteArray): Int {
    for (i in 0 until minOf(a.size, b.size)) {
        val difference = (a[i].toInt() and 0xff) - (b[i].toInt() and 0xff)
        if (difference != 0) return difference
    }
    return a.size - b.size
}

private const val MAJOR_BYTES = 2
private const val MAJOR_TEXT = 3
private const val MAJOR_ARRAY = 4
private const val MAJOR_MAP = 5
private const val MAJOR_TAG = 6
private const val MAJOR_SIMPLE = 7

private data class CborHead(val major: Int, val argument: Long)

/** Reads definite-length CBOR items, failing on anything else, e.g. indefinite lengths. */
private class CborReader(private val bytes: ByteArray) {
    var position = 0
        private set

    fun readHead(): CborHead {
        val initial = bytes[position++].toInt() and 0xff
        val major = initial shr 5
        val info = initial and 0x1f
        val argument = when {
            info < 24 -> info.toLong()
            info <= 27 -> (0 until (1 shl (info - 24))).fold(0L) { acc, _ ->
                (acc shl 8) or (bytes[position++].toLong() and 0xff)
            }

            else -> throw IllegalArgumentException("Unsupported CBOR additional information $info")
        }
        require(argument >= 0) { "CBOR argument out of range" }
        return CborHead(major, argument)
    }

    fun readBytes(count: Int): ByteArray {
        require(count >= 0 && position + count <= bytes.size) { "CBOR item exceeds its input" }
        return bytes.copyOfRange(position, position + count).also { position += count }
    }

    /** The encoding of the next item, including nested items. */
    fun readItem(): ByteArray {
        val start = position
        skipItem()
        return bytes.copyOfRange(start, position)
    }

    private fun skipItem() {
        val head = readHead()
        when (head.major) {
            MAJOR_BYTES, MAJOR_TEXT -> readBytes(head.argument.toInt())
            MAJOR_ARRAY -> repeat(head.argument.toInt()) { skipItem() }
            MAJOR_MAP -> repeat(head.argument.toInt() * 2) { skipItem() }
            MAJOR_TAG -> skipItem()
            MAJOR_SIMPLE, 0, 1 -> Unit
        }
    }
}

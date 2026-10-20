package at.asitplus.wallet.app.common.trust

import okio.Buffer
import okio.Inflater
import okio.InflaterSource
import okio.buffer
import okio.use

/**
 * Minimal reader for the files of a ZIP archive held in memory, enough for bundles of certificates: stored and
 * deflated entries, no encryption, no ZIP64, no spanned archives.
 */
internal object ZipArchive {
    private const val LOCAL_FILE_HEADER = 0x04034b50
    private const val CENTRAL_DIRECTORY_HEADER = 0x02014b50
    private const val END_OF_CENTRAL_DIRECTORY = 0x06054b50
    private const val END_OF_CENTRAL_DIRECTORY_SIZE = 22
    private const val METHOD_STORED = 0
    private const val METHOD_DEFLATED = 8
    private const val FLAG_ENCRYPTED = 0x1

    /** Upper bound for the size of a single file, so that a malicious archive cannot exhaust the memory. */
    private const val MAX_ENTRY_SIZE = 1L shl 20

    fun isZip(bytes: ByteArray): Boolean = bytes.size >= 4 && bytes.int(0) == LOCAL_FILE_HEADER

    /**
     * The files in [zip] by their path, with `/` as separator also for archives created with `\`, as on Windows.
     * Directories are left out.
     */
    fun files(zip: ByteArray): Map<String, ByteArray> {
        val end = findEndOfCentralDirectory(zip)
        val count = zip.short(end + 10)
        var offset = zip.int(end + 16)
        return buildMap {
            repeat(count) {
                require(zip.int(offset) == CENTRAL_DIRECTORY_HEADER) { "Invalid ZIP directory entry" }
                val flags = zip.short(offset + 8)
                val method = zip.short(offset + 10)
                val compressedSize = zip.int(offset + 20).toUInt().toLong()
                val size = zip.int(offset + 24).toUInt().toLong()
                val nameLength = zip.short(offset + 28)
                val extraLength = zip.short(offset + 30)
                val commentLength = zip.short(offset + 32)
                val localHeader = zip.int(offset + 42).toUInt().toLong()
                require(offset + 46 + nameLength <= zip.size) { "Truncated ZIP" }
                val name = zip.decodeToString(offset + 46, offset + 46 + nameLength).replace('\\', '/')
                offset += 46 + nameLength + extraLength + commentLength

                if (name.endsWith('/')) return@repeat
                require(flags and FLAG_ENCRYPTED == 0) { "Encrypted ZIP entry $name" }
                require(size <= MAX_ENTRY_SIZE && compressedSize <= MAX_ENTRY_SIZE) { "ZIP entry $name too large" }
                put(name, readEntry(zip, localHeader.toInt(), method, compressedSize.toInt(), size.toInt(), name))
            }
        }
    }

    private fun readEntry(zip: ByteArray, header: Int, method: Int, compressedSize: Int, size: Int, name: String) =
        run {
            require(zip.int(header) == LOCAL_FILE_HEADER) { "Invalid local header of ZIP entry $name" }
            val start = header + 30 + zip.short(header + 26) + zip.short(header + 28)
            require(start >= 0 && start + compressedSize <= zip.size) { "ZIP entry $name exceeds the archive" }
            val data = Buffer().write(zip, start, compressedSize)
            when (method) {
                METHOD_STORED -> data.readByteArray()
                // Raw DEFLATE, without ZLIB header
                METHOD_DEFLATED -> InflaterSource(data, Inflater(true)).buffer().use { inflated ->
                    // Reads one byte more than announced to detect an archive lying about the size of an entry.
                    val result = Buffer()
                    while (result.size <= size && inflated.read(result, 8192) != -1L) Unit
                    require(result.size == size.toLong()) { "Size of ZIP entry $name does not match" }
                    result.readByteArray()
                }

                else -> throw IllegalArgumentException("Unsupported compression method $method of ZIP entry $name")
            }
        }

    private fun findEndOfCentralDirectory(zip: ByteArray): Int {
        // Followed by a comment of at most 64 KiB.
        val last = zip.size - END_OF_CENTRAL_DIRECTORY_SIZE
        val first = maxOf(0, last - 0xFFFF)
        return (last downTo first).firstOrNull { zip.int(it) == END_OF_CENTRAL_DIRECTORY }
            ?: throw IllegalArgumentException("No ZIP end of central directory")
    }

    private fun ByteArray.short(index: Int): Int {
        require(index >= 0 && index + 2 <= size) { "Truncated ZIP" }
        return (this[index].toInt() and 0xFF) or ((this[index + 1].toInt() and 0xFF) shl 8)
    }

    private fun ByteArray.int(index: Int): Int {
        require(index >= 0 && index + 4 <= size) { "Truncated ZIP" }
        return short(index) or (short(index + 2) shl 16)
    }
}

package data.storage

import androidx.datastore.core.InterProcessCoordinator
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.F_SETLK
import platform.posix.F_SETLKW
import platform.posix.F_UNLCK
import platform.posix.F_WRLCK
import platform.posix.O_CREAT
import platform.posix.O_RDWR
import platform.posix.SEEK_SET
import platform.posix.close
import platform.posix.fcntl
import platform.posix.open
import platform.posix.posix_errno

private const val LOCK_FILE_PERMISSIONS = 0x180 // 0600
private const val UPDATE_POLL_INTERVAL_MS = 250L

/**
 * Minimal iOS analogue of AndroidX DataStore's Android multi-process coordinator.
 *
 * DataStore increments the shared version before writing the file, then relies on a file-change
 * notification after the atomic move to make other processes re-read. iOS has no Android-style
 * FileObserver in DataStore, so this coordinator combines a POSIX advisory lock/version sidecar
 * with polling of the actual DataStore file's metadata.
 */
internal class DarwinInterProcessCoordinator(
    private val fileSystem: FileSystem,
    private val dataPath: Path,
) : InterProcessCoordinator {
    private val inMemoryMutex = Mutex()
    private val lockPath = "$dataPath.lock".toPath()
    private val versionPath = "$dataPath.version".toPath()

    override val updateNotifications: Flow<Unit> = flow {
        emit(Unit)
        var lastStamp = dataFileStamp()
        while (currentCoroutineContext().isActive) {
            delay(UPDATE_POLL_INTERVAL_MS)
            val currentStamp = dataFileStamp()
            if (currentStamp != lastStamp) {
                lastStamp = currentStamp
                emit(Unit)
            }
        }
    }

    override suspend fun <T> lock(block: suspend () -> T): T = inMemoryMutex.withLock {
        withFileLock(wait = true) { locked ->
            check(locked) { "Could not acquire DataStore file lock for $dataPath" }
            block()
        }
    }

    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        if (!inMemoryMutex.tryLock()) {
            return block(false)
        }
        try {
            return withFileLock(wait = false, block = block)
        } finally {
            inMemoryMutex.unlock()
        }
    }

    override suspend fun getVersion(): Int = readVersion()

    override suspend fun incrementAndGetVersion(): Int {
        val next = when (val current = readVersion()) {
            Int.MAX_VALUE -> 1
            else -> current + 1
        }
        writeVersion(next)
        return next
    }

    private fun dataFileStamp(): Long {
        return fileSystem.metadataOrNull(dataPath)?.lastModifiedAtMillis ?: -1L
    }

    private fun readVersion(): Int {
        if (!fileSystem.exists(versionPath)) {
            return 0
        }
        return try {
            fileSystem.read(versionPath) {
                readUtf8().trim().toIntOrNull() ?: 0
            }
        } catch (e: IOException) {
            if (fileSystem.exists(versionPath)) throw e else 0
        }
    }

    private fun writeVersion(version: Int) {
        ensureParentDirectory()
        fileSystem.write(versionPath, mustCreate = false) {
            writeUtf8(version.toString())
        }
    }

    private suspend fun <T> withFileLock(wait: Boolean, block: suspend (Boolean) -> T): T {
        ensureParentDirectory()
        val fd = open(lockPath.toString(), O_CREAT or O_RDWR, LOCK_FILE_PERMISSIONS)
        if (fd == -1) {
            throw IOException("Could not open DataStore lock file $lockPath: errno=${posix_errno()}")
        }

        val locked = setFileLock(fd, wait)
        try {
            return block(locked)
        } finally {
            if (locked) {
                unlockFile(fd)
            }
            close(fd)
        }
    }

    private fun ensureParentDirectory() {
        dataPath.parent?.let { fileSystem.createDirectories(it, mustCreate = false) }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setFileLock(fd: Int, wait: Boolean): Boolean = memScoped {
        val lock = alloc<platform.posix.flock>()
        lock.l_type = F_WRLCK.convert()
        lock.l_whence = SEEK_SET.convert()
        lock.l_start = 0
        lock.l_len = 0
        lock.l_pid = 0

        val command = if (wait) F_SETLKW else F_SETLK
        val result = fcntl(fd, command, lock.ptr)
        if (result == 0) {
            true
        } else if (wait) {
            throw IOException("Could not acquire DataStore file lock for $lockPath: errno=${posix_errno()}")
        } else {
            false
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun unlockFile(fd: Int) = memScoped {
        val lock = alloc<platform.posix.flock>()
        lock.l_type = F_UNLCK.convert()
        lock.l_whence = SEEK_SET.convert()
        lock.l_start = 0
        lock.l_len = 0
        lock.l_pid = 0
        fcntl(fd, F_SETLK, lock.ptr)
    }
}

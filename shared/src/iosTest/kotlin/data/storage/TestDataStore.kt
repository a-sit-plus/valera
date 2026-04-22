package data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSFileManager
import platform.Foundation.NSUUID

@OptIn(ExperimentalForeignApi::class)
fun createTestDataStore(): DataStore<Preferences> {
    val basePath = requireNotNull(NSFileManager.defaultManager.temporaryDirectory.path)
    val fileName = "test-${NSUUID().UUIDString}-$dataStoreFileName"
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { "$basePath/$fileName".toPath() }
    )
}

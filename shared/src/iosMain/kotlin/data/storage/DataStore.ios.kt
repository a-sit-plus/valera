package data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

// Modified from https://github.com/android/kotlin-multiplatform-samples/tree/main
@OptIn(ExperimentalForeignApi::class)
fun createDataStore(): DataStore<Preferences> = getDataStore(
    producePath = {
        val appGroupIdentifier = "group.Compose.Wallet"
        val containerURL: NSURL? = NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(appGroupIdentifier)
        requireNotNull(containerURL) {
            "Could not get container URL for app group $appGroupIdentifier. Make sure the app group is configured correctly."
        }.path + "/$dataStoreFileName"
    }
)
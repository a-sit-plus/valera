package data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

private const val appGroupIdentifierInfoPlistKey = "WalletAppGroupIdentifier"

// Modified from https://github.com/android/kotlin-multiplatform-samples/tree/main
@OptIn(ExperimentalForeignApi::class)
fun createDataStore(): DataStore<Preferences> = getDataStore(
    producePath = {
        resolveCurrentDataStorePath().also { currentPath ->
            migrateLegacyDataStoreIfNeeded(currentPath)
        }
    }
)

@OptIn(ExperimentalForeignApi::class)
private fun resolveCurrentDataStorePath(): String {
    val appGroupIdentifier = getAppGroupIdentifier()
    val containerURL: NSURL? =
        NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(appGroupIdentifier)
    requireNotNull(containerURL) {
        "Could not get container URL for app group $appGroupIdentifier. Make sure the app group is configured correctly."
    }
    return requireNotNull(containerURL.path) + "/$dataStoreFileName"
}

@OptIn(ExperimentalForeignApi::class)
private fun migrateLegacyDataStoreIfNeeded(currentPath: String) {
    val legacyPath = resolveLegacyDataStorePath()
    val fileManager = NSFileManager.defaultManager

    if (legacyPath == currentPath) return
    if (!fileManager.fileExistsAtPath(legacyPath)) return
    if (fileManager.fileExistsAtPath(currentPath)) return

    if (fileManager.moveItemAtPath(legacyPath, currentPath, null)) {
        Napier.i("Migrated iOS DataStore from legacy path to app group container")
    } else {
        Napier.w("Failed to migrate iOS DataStore from $legacyPath to $currentPath")
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun resolveLegacyDataStorePath(): String {
    val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path) + "/$dataStoreFileName"
}

@OptIn(ExperimentalForeignApi::class)
private fun getAppGroupIdentifier(): String {
    val appGroupIdentifier = NSBundle.mainBundle.objectForInfoDictionaryKey(appGroupIdentifierInfoPlistKey) as? String
    if (!appGroupIdentifier.isNullOrBlank()) {
        return appGroupIdentifier
    }

    error(
        "Could not resolve app group identifier from Info.plist key $appGroupIdentifierInfoPlistKey. " +
            "Configure APP_GROUP_IDENTIFIER in iosApp/Configuration/Config.xcconfig or Signing.local.xcconfig."
    )
}

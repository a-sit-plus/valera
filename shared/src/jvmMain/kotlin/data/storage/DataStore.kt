package data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences


// Modified from https://github.com/android/kotlin-multiplatform-samples/tree/main
fun createDataStore(): DataStore<Preferences> = getDataStore(
    producePath = {
        dataStoreFileName
    }
)
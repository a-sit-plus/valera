
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlin.test.Test
import kotlin.test.assertIs
class IosTests {
    // Just an dummy test
    @Test
    fun testDataStore() {
        val result = createDataStore()
        assertIs<DataStore<Preferences>>(result)
    }
}
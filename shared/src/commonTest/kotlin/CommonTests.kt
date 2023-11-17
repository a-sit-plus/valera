import data.storage.IdHolder
import kotlin.test.Test
import kotlin.test.assertIs

class CommonTests {
    // Just an dummy test
    @Test
    fun testIdHolder() {
        val results = IdHolder()
        assertIs<IdHolder>(results)
    }
}
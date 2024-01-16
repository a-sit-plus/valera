import kotlin.test.Test
import kotlin.test.assertIs


class UnitTests {
    // Just a dummy test
    @Test
    fun testObjectFactory() {
        val result = AndroidObjectFactory()
        assertIs<AndroidObjectFactory>(result)
    }
}
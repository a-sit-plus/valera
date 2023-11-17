import kotlin.test.Test
import kotlin.test.assertIs


class AndroidTests {
    // Just a dummy test
    @Test
    fun testObjectFactory() {
        val result = AndroidObjectFactory()
        assertIs<AndroidObjectFactory>(result)
    }
}
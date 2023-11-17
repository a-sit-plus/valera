import kotlin.test.Test
import kotlin.test.assertIs


class AndroidTests {
    // Just an dummy test
    @Test
    fun testObjectFactory() {
        val result = AndroidObjectFactory()
        assertIs<AndroidObjectFactory>(result)
    }
}
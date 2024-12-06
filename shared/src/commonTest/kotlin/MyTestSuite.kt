
import androidx.compose.runtime.Composable
import io.kotest.core.spec.style.FunSpec
import kotlin.test.Test

class MyTestSuite : FunSpec({

    include(instrumentedTests())


})
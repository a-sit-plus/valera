import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.TrustListService
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class TrustListFetcherTest(
    private val trustListService: TrustListService
) {
    @Test
    fun fetchPidTrustList() {
        val res = runBlocking { trustListService.fetchTrustList() }

        println(res?.trustedEntitiesList)
    }
}
package at.asitplus.wallet.app.common.domain.vck

import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.josef.JsonWebKeySet
import at.asitplus.wallet.app.common.HttpService
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.jvm.JvmInline

@JvmInline
value class JsonWebKeySetResolver(
    val httpService: HttpService
) {
    suspend operator fun invoke(url: String): JsonWebKeySet? = withContext(Dispatchers.IO) {
        catchingUnwrapped {
            httpService.buildHttpClient().get(url).body<JsonWebKeySet>()
        }.getOrNull()
    }
}

package data.trustlist

import androidx.compose.runtime.Composable
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

expect fun getTrustListService(): TrustListService


interface TrustListService {
    fun setContext(context: Any)

    @Composable
    fun setContext()

    fun fetchAndStoreTrustedFingerprints()

    fun getTrustedFingerprints(): List<String>?
}
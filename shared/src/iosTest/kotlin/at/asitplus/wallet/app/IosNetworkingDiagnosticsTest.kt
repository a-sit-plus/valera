package at.asitplus.wallet.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import kotlin.test.Test

class IosNetworkingDiagnosticsTest {

    @Test
    fun printTlsDiagnostics() = runBlocking {
        val processInfo = NSProcessInfo.processInfo
        println("IosNetworkingDiagnostics: date=${NSDate()}")
        println("IosNetworkingDiagnostics: os=${processInfo.operatingSystemVersionString}")
        println("IosNetworkingDiagnostics: host=${processInfo.hostName}")

        val environment = processInfo.environment
        listOf(
            "DEVELOPER_DIR",
            "SIMULATOR_DEVICE_NAME",
            "SIMULATOR_MODEL_IDENTIFIER",
            "SIMULATOR_RUNTIME_VERSION",
            "SIMULATOR_ROOT",
        ).forEach { key ->
            println("IosNetworkingDiagnostics: env[$key]=${environment[key]}")
        }

        val client = HttpClient(Darwin) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
        }

        val urls = listOf(
            "https://apple.com",
            "https://wallet.a-sit.at",
            "https://apps.egiz.gv.at",
        )

        try {
            urls.forEach { url ->
                runCatching {
                    val response = client.get(url)
                    val bodyPreview = runCatching { response.bodyAsText().take(120) }
                        .getOrElse { "<body unavailable: ${it::class.simpleName}: ${it.message}>" }
                    println(
                        "IosNetworkingDiagnostics: GET $url -> " +
                            "status=${response.status.value} " +
                            "description=${response.status.description} " +
                            "bodyPreview=$bodyPreview"
                    )
                }.onFailure {
                    println(
                        "IosNetworkingDiagnostics: GET $url failed: " +
                            "${it::class.simpleName}: ${it.message}"
                    )
                    it.printStackTrace()
                }
            }
        } finally {
            client.close()
        }
    }
}

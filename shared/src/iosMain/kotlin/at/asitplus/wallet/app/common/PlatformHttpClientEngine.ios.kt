package at.asitplus.wallet.app.common

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun createPlatformHttpClientEngine(): HttpClientEngine = Darwin.create()

package at.asitplus.wallet.app.common

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

internal actual fun createPlatformHttpClientEngine(): HttpClientEngine = CIO.create()

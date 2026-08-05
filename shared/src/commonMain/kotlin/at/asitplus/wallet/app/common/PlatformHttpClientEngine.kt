package at.asitplus.wallet.app.common

import io.ktor.client.engine.HttpClientEngine

/** Creates the native HTTP engine used by wallet clients on the current platform. */
internal expect fun createPlatformHttpClientEngine(): HttpClientEngine

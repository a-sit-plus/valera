package at.asitplus.wallet.app.common

/**
 * Whether this process is an app extension, i.e. the iOS Identity Document Provider extension, instead of the app.
 * It answers a single DC API request under a much lower memory limit than the app.
 */
internal expect fun isAppExtension(): Boolean

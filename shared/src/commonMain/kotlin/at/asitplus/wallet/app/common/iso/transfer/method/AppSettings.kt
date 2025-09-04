package at.asitplus.wallet.app.common.iso.transfer.method

internal expect class AppSettings() {
    fun open(platformContext: PlatformContext)
}

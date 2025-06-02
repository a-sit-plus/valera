package at.asitplus.wallet.app.common.domain.platform

fun interface UrlOpener {
    operator fun invoke(url: String)
}
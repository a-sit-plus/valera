package at.asitplus.wallet.app.common

class SigningService(
    platformAdapter: PlatformAdapter,
    httpService: HttpService
) {
    private val client = httpService.buildHttpClient()

    fun sign() {
        println("SigningService: sign()")
    }
}
package at.asitplus.wallet.app.dcapi

data class IosDcApiPreRequestData(
    val parsedRequestSummary: String?,
    val origin: String?,
    /**
     * The reader authentication certificate chains of the parsed request, leaf first, each certificate DER encoded
     * in Base64. The system has verified their signatures over the request before handing it to the extension, but
     * the raw request is only released after consent, and is validated again then.
     */
    val readerCertificateChains: List<List<String>>,
    val onContinue: () -> Unit,
    val onCancel: () -> Unit,
)

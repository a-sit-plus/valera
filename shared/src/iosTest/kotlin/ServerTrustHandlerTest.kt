import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.NSURLSessionTask
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust
import platform.Security.SecTrustCopyCertificateChain
import platform.Security.SecTrustRef
import platform.Security.SecTrustSetAnchorCertificates
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFErrorRefVar
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Security.SecTrustEvaluate
import platform.Security.SecTrustEvaluateWithError
import platform.Security.SecTrustResultTypeVar
import platform.Security.errSecSuccess
import platform.Security.kSecTrustResultInvalid
import platform.Security.kSecTrustResultProceed
import platform.Security.kSecTrustResultUnspecified
import platform.darwin.NSInteger

/**
 * A handler for testing with self-signed certificates on iOS.
 * It is used to suppress DarwinHttpRequestException when connecting to apps.egiz.gv.at.
 * SECURITY WARNING:
 * Method trustIsValid() always returns true, effectively accepting any certificate.
 * This is intentional for testing purposes but is unsafe for production use.
 *
 * @see <a href="https://alistairsykes.medium.com/how-to-trust-all-ssl-certificates-with-kmm-and-ktor-8f686aec0d09">Trust all ssl certificates</a>
 */
@OptIn(ExperimentalForeignApi::class)
class ServerTrustHandlerTest {

    /**
     * Handles the server trust challenge during an HTTPS connection.
     * This method evaluates the server's certificate and decides whether to accept or reject it.
     * For testing purpose, it will accept any certificate.
     */
    fun handleChallenge(
        session: NSURLSession,
        task: NSURLSessionTask,
        challenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
    ) {
        val protectionSpace = challenge.protectionSpace
        if (protectionSpace.authenticationMethod != NSURLAuthenticationMethodServerTrust) {
            completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
            return
        }

        val serverTrust = protectionSpace.serverTrust
        if (serverTrust == null) {
            completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
            return
        }

        val certChain = SecTrustCopyCertificateChain(serverTrust)
        SecTrustSetAnchorCertificates(serverTrust, certChain)

        if (serverTrust.trustIsValid()) {
            val credential = NSURLCredential.credentialForTrust(serverTrust)
            completionHandler(NSURLSessionAuthChallengeUseCredential as NSInteger, credential)
        } else {
            completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge as NSInteger, null)
        }

    }


    /**
     * Evaluates the trust of a server certificate.
     *
     * @return Always returns true, accepting any certificate
     */
    private fun SecTrustRef.trustIsValid(): Boolean {
        var isValid = false

        val version = cValue<NSOperatingSystemVersion> {
            majorVersion = 12
            minorVersion = 0
            patchVersion = 0
        }

        if (NSProcessInfo().isOperatingSystemAtLeastVersion(version)) {
            memScoped {
                val result = alloc<CFErrorRefVar>()
                // https://developer.apple.com/documentation/security/2980705-sectrustevaluatewitherror
                isValid = SecTrustEvaluateWithError(this@trustIsValid, result.ptr)
            }
        } else {
            // https://developer.apple.com/documentation/security/1394363-sectrustevaluate
            memScoped {
                val result = alloc<SecTrustResultTypeVar>()
                result.value = kSecTrustResultInvalid
                val status = SecTrustEvaluate(this@trustIsValid, result.ptr)
                if (status == errSecSuccess) {
                    isValid = result.value == kSecTrustResultUnspecified ||
                            result.value == kSecTrustResultProceed
                }
            }
        }
        return true
    }


}


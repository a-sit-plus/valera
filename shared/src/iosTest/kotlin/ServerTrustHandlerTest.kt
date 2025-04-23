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
import io.ktor.client.engine.darwin.ChallengeHandler
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.internal.ConstantValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFErrorCopyDescription
import platform.CoreFoundation.CFErrorCopyFailureReason
import platform.CoreFoundation.CFErrorCopyRecoverySuggestion
import platform.CoreFoundation.CFErrorCopyUserInfo
import platform.CoreFoundation.CFErrorRefVar
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Security.SecTrustEvaluate
import platform.Security.SecTrustEvaluateWithError
import platform.Security.SecTrustResultTypeVar
import platform.Security.SecTrustSetAnchorCertificates
import platform.Security.errSecSuccess
import platform.Security.kSecTrustResultInvalid
import platform.Security.kSecTrustResultProceed
import platform.Security.kSecTrustResultUnspecified
import platform.darwin.NSInteger

@OptIn(ExperimentalForeignApi::class)
class ServerTrustHandlerTest {

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
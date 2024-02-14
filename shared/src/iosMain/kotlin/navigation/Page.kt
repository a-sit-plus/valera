package navigation

actual class HomePage : Page

actual class AboutPage : Page

actual class LogPage : Page

actual class CredentialPage actual constructor(actual val info: String) : Page

actual class CameraPage : Page

actual class PayloadPage actual constructor(actual val info: String) : Page

actual class ConsentPage actual constructor(actual val claims: List<String>) : Page

actual class LoadingPage : Page
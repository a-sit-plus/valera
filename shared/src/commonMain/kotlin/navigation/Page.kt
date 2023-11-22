package navigation

interface Page
expect class HomePage() : Page

expect class AboutPage() : Page

expect class CredentialPage(info: String) : Page {
    val info: String
}

expect class CameraPage() : Page
expect class PayloadPage(info: String) : Page {
    val info: String
}
expect class AppLinkPage(info: String) : Page {
    val info: String
}
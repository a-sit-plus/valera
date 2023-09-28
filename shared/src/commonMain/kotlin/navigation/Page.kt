package navigation

interface Page
expect class HomePage() : Page

expect class AboutPage() : Page

expect class CredentialPage(info: Int) : Page {
    val info: Int
}

expect class CameraPage() : Page
expect class PayloadPage(info: String) : Page {
    val info: String
}
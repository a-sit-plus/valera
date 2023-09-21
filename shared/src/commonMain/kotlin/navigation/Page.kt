package navigation

interface Page
expect class HomePage() : Page

expect class AboutPage() : Page

expect class CredentialPage() : Page

expect class CameraPage() : Page
expect class PayloadPage(info: String) : Page {
    val info: String
}
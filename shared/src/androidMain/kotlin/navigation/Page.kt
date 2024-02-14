package navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
actual class HomePage : Page, Parcelable

@Parcelize
actual class AboutPage : Page, Parcelable

@Parcelize
actual class LogPage : Page, Parcelable

@Parcelize
actual class CredentialPage actual constructor(actual val info: String) : Page, Parcelable

@Parcelize
actual class CameraPage : Page, Parcelable

@Parcelize
actual class PayloadPage actual constructor(actual val info: String) : Page, Parcelable

@Parcelize
actual class ConsentPage actual constructor(actual val claims: List<String>): Page, Parcelable

@Parcelize
actual class LoadingPage : Page, Parcelable
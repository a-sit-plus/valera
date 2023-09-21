package navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
actual class HomePage : Page, Parcelable

@Parcelize
actual class AboutPage : Page, Parcelable

@Parcelize
actual class CredentialPage : Page, Parcelable
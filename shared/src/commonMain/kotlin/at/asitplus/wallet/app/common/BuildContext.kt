package at.asitplus.wallet.app.common

data class BuildContext(
    val buildType: BuildType,
    /** Something like `at.asitplus.wallet.app.android` */
    val packageName: String,
    /** Something like `50140` */
    val versionCode: Int,
    /** Something like `5.4.0` */
    val versionName: String,
    /** Something like `Android 15` or `iOS 16` */
    val osVersion: String,
)

enum class BuildType(val stringRepresentaiton: String) {
    DEBUG("debug"),
    RELEASE("release")
}
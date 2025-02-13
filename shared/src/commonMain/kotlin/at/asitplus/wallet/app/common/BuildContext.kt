package at.asitplus.wallet.app.common

data class BuildContext(
    val buildType: BuildType,
    val packageName: String,
    val versionCode: Int,
    val versionName: String,
    val operatingSystemString: String,
)

enum class BuildType(val stringRepresentaiton: String) {
    DEBUG("debug"),
    RELEASE("release")
}
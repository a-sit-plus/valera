package at.asitplus.wallet.app.common

enum class BuildEnvironment(val abbreviation: String) {
    Development("D"),
    QualityAssurance("Q"),
    Production("P");
}

data class BuildContext(
    val buildType: BuildType,
    val packageName: String,
    val versionCode: Int,
    val versionName: String,
)

enum class BuildType(val stringRepresentaiton: String) {
    DEBUG("debug"),
    RELEASE("release")
}
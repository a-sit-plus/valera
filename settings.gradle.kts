include(":androidApp")
include(":shared")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://raw.githubusercontent.com/a-sit-plus/gradle-conventions-plugin/mvn/repo")
    }

    plugins {
        val agpVersion = extra["agp.version"] as String
        id("com.android.application").version(agpVersion)
        id("com.android.library").version(agpVersion)
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.4.0")
}


val vckDir= file("../vck")
val signumFile=file("../vck/signum/build.gradle.kts")
if (vckDir.isDirectory && signumFile.exists()) {
    logger.warn("Detected VC-K in ${vckDir.absolutePath}.")
    logger.warn("Including VC-K and Signum as composite build.")
    logger.warn("If you do not want this, move the VC-K to another location!")
    includeBuild("../vck")
}

val vckVersion :String get() = settings.extra["vck.version"].toString()

dependencyResolutionManagement {

    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }

    versionCatalogs {
        create("vckOidCatalog") {
            from("at.asitplus.wallet:vck-openid-ktor-versionCatalog:$vckVersion")
        }
    }
    //because the other one does not provide the transitive VC-K dependency required for XFC export
   versionCatalogs {
        create("vckCatalog") {
            from("at.asitplus.wallet:vck-versionCatalog:$vckVersion")
        }
    }

}

rootProject.name = "ComposeWalletApp"

include(":androidApp")
include(":shared")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://raw.githubusercontent.com/a-sit-plus/gradle-conventions-plugin/mvn/repo")
    }

    plugins {
        val agpVersion = extra["agp.version"] as String
        val composeVersion = extra["compose.version"] as String
        id("com.android.application").version(agpVersion)
        id("com.android.library").version(agpVersion)
        id("org.jetbrains.compose").version(composeVersion)
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.4.0")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    versionCatalogs {
        create("signumCatalog") {
            from("at.asitplus.signum:indispensable-versionCatalog:3.6.0")
        }

        create("vclibCatalog") {
            from("at.asitplus.wallet:vck-openid-versionCatalog:4.1.1")
        }
    }
}

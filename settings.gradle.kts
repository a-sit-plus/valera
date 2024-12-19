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
 //       val composeVersion = extra["compose.version"] as String
        id("com.android.application").version(agpVersion)
        id("com.android.library").version(agpVersion)
   //     id("org.jetbrains.compose").version(composeVersion)
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.4.0")
}

val vckVersion :String get() = settings.extra["vck.version"].toString()

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots") //Version catalog and kotest snapshot
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

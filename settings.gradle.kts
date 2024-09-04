import org.apache.tools.ant.taskdefs.condition.Os

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

if (System.getProperty("publishing.excludeIncludedBuilds") != "true") {
    includeBuild("vck") {
        dependencySubstitution {
            substitute(module("at.asitplus.wallet:vck")).using(project(":vck"))
            substitute(module("at.asitplus.wallet:vck-openid")).using(project(":vck-openid"))
            substitute(module("at.asitplus.wallet:vck-aries")).using(project(":vck-aries"))
        }
    }
} else logger.lifecycle("Excluding Signum from this build")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven {
            url = uri("file:./vck/repo")
            name = "vck + signum"
        }
    }
    if (!File("${rootDir.absolutePath}/vck/repo/at/asitplus/wallet/vck-openid-versionCatalog/4.2.0-SNAPSHOT/maven-metadata.xml").exists()) {
        logger.lifecycle("building VC-K and Signum for version catalogs. this will take a long time!")
        kotlin.runCatching {
            file("local.properties").also { src ->

                src.copyTo(
                    file("${rootDir.absolutePath}/vck/local.properties"),
                    overwrite = true
                )
                src.copyTo(
                    file("${rootDir.absolutePath}/vck/signum/local.properties"),
                    overwrite = true
                )
            }
        }
        exec {
            workingDir = File("${rootDir.absolutePath}/vck/signum")

            commandLine(
                if (!Os.isFamily(Os.FAMILY_WINDOWS)) "./gradlew" else "./gradlew.bat",
                "publishAllPublicationsToLocalRepository"
            )
        }
        exec {
            workingDir = File("${rootDir.absolutePath}/vck")
            commandLine(
                if (!Os.isFamily(Os.FAMILY_WINDOWS)) "./gradlew" else "./gradlew.bat",
                "publishAllPublicationsToLocalRepository"
            )
        }

        file("./vck/signum/repo").copyRecursively(
            file("./vck/repo"),
            overwrite = true
        )

    }
    versionCatalogs {
        create("signumCatalog") {
            from("at.asitplus.signum:indispensable-versionCatalog:3.7.0-SNAPSHOT")
        }

        create("vclibCatalog") {
            from("at.asitplus.wallet:vck-openid-versionCatalog:4.2.0-SNAPSHOT")
        }
    }

}

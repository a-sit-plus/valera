plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    id("com.android.application").apply(false)
    id("com.android.library").apply(false)
    id("at.asitplus.gradle.conventions") version "2.0.20+20240829"

    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
}

task("prepareSupreme") {
    doFirst {
        kotlin.runCatching {
            rootProject.layout.projectDirectory.file("local.properties").asFile.also { src ->
                src.copyTo(
                    rootProject.layout.projectDirectory.dir("vck").file("local.properties").asFile,
                    overwrite = true
                )
                src.copyTo(
                    rootProject.layout.projectDirectory.dir("vck").dir("signum")
                        .file("local.properties").asFile,
                    overwrite = true

                )

            }
        }
        exec {
            workingDir = File("${rootDir}/vck/signum")
            commandLine("./gradlew", "publishAllPublicationsToLocalRepository")
        }
        exec {
            workingDir = File("${rootDir}/vck")
            commandLine("./gradlew", "publishAllPublicationsToLocalRepository")
        }

    }
    doLast {
        rootProject.layout.projectDirectory.dir("vck").dir("signum")
            .dir("repo").asFile.copyRecursively(
                rootProject.layout.projectDirectory.dir("vck").dir("repo").asFile,
                overwrite = true
            )

    }
}

repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")

}

allprojects {
    repositories {
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven(url = uri("https://s01.oss.sonatype.org/content/repositories/atasitplus-1173/"))
        maven(url = uri("https://s01.oss.sonatype.org/content/repositories/atasitplus-1175/"))

        maven {
            url = uri(rootProject.layout.projectDirectory.dir("vck").dir("repo"))
            name = "vck + signum"
        }
    }
}


plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    id("com.android.application").apply(false)
    id("com.android.library").apply(false)
    id("at.asitplus.gradle.conventions") version "2.1.0+20241219"

    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) version (vckOidCatalog.versions.kotlin) apply false
}

repositories {
    mavenLocal()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")

}

allprojects {
    repositories {
        mavenLocal()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}


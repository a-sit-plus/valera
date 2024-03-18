plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    id("com.android.application").apply(false)
    id("com.android.library").apply(false)
    id("org.jetbrains.compose").apply(false)
    id("at.asitplus.gradle.conventions") version "1.9.23+20240318"
}

allprojects {
    repositories {
        mavenLocal()
    }
}
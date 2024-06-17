plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("at.asitplus.gradle.conventions")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()
    sourceSets {
        androidMain {
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}

val apkSignerPassword =
    (findProperty("android.cert.password") as String?) ?: System.getenv("ANDROID_CERT_PASSWORD")

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "at.asitplus.wallet.app.android"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    signingConfigs {
        getByName("debug") {
            storeFile = file("keystore.p12")
            storePassword = apkSignerPassword
            keyAlias = "key0"
            keyPassword = apkSignerPassword
        }
        create("release") {
            storeFile = file("keystore.p12")
            storePassword = apkSignerPassword
            keyAlias = "key0"
            keyPassword = apkSignerPassword
        }
    }
    defaultConfig {
        applicationId = "at.asitplus.wallet.app.android"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = (findProperty("version.code") as String).toInt()
        versionName = findProperty("version.name") as String
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        getByName("debug") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        resources.excludes.add("META-INF/versions/9/previous-compilation-data.bin")
    }
}

repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}
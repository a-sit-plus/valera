plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}

android {
    signingConfigs {
        getByName("debug") {
            storeFile = file("signer.jks")
            storePassword = "changeit"
            keyAlias = "key1"
            keyPassword = "changeit"
        }
    }
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "at.asitplus.wallet.app.android"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    signingConfigs {
        create("release") {
            storeFile =
                file("signer.jks")
            storePassword = "changeit"
            keyAlias = "key1"
            keyPassword = "changeit"
        }
        if (System.getenv("CI") != null) {
            create("github") {
                storeFile = file("keystore.p12")
                storePassword = System.getenv("ANDROID_CERT_PASSWORD")
                keyAlias = "key0"
                keyPassword = System.getenv("ANDROID_CERT_PASSWORD")
            }
        }
    }
    defaultConfig {
        applicationId = "at.asitplus.wallet.app.android"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = 1
        versionName = "1.0"
        if (System.getenv("CI") != null) {
            signingConfig = signingConfigs.getByName("github")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

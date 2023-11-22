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
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "at.asitplus.wallet.app.android"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    signingConfigs {
        getByName("debug") {
            storeFile = file("keystore.p12")
            storePassword = (findProperty("android.cert.password") as String?) ?: System.getenv("ANDROID_CERT_PASSWORD")
            keyAlias = "key0"
            keyPassword = findProperty("android.cert.password") as String? ?: System.getenv("ANDROID_CERT_PASSWORD")
        }
        create("release") {
            storeFile = file("keystore.p12")
            storePassword = findProperty("android.cert.password") as String? ?: System.getenv("ANDROID_CERT_PASSWORD")
            keyAlias = "key0"
            keyPassword = findProperty("android.cert.password") as String? ?: System.getenv("ANDROID_CERT_PASSWORD")
        }
    }
    defaultConfig {
        applicationId = "at.asitplus.wallet.app.android"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = 1
        versionName = "1.0"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    packaging {
        resources.excludes.add("META-INF/versions/9/previous-compilation-data.bin")
    }
}
